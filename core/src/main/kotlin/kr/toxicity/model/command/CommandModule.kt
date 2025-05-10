package kr.toxicity.model.command

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.commandsenders.BukkitCommandSender
import dev.jorel.commandapi.executors.CommandExecutionInfo
import dev.jorel.commandapi.executors.ExecutionInfo
import kr.toxicity.model.util.audience
import kr.toxicity.model.util.info
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender

fun commandModule(name: String, block: CommandAPICommand.() -> Unit) = CommandModule(null, CommandAPICommand(name).apply(block))

class CommandModule(
    parent: CommandModule?,
    private val delegate: CommandAPICommand
) : CommandExecutionInfo {
    companion object {
        private val lineMessage by lazy {
            Component.text()
                .content("----------------------------------------")
                .color(NamedTextColor.GRAY)
                .build()
        }
        private val requiredMessage by lazy {
            Component.text()
                .append(Component.text("    <argument>").color(NamedTextColor.RED))
                .append(Component.space())
                .append(Component.text(" - required"))
                .build()
        }
        private val optionalMessage by lazy {
            Component.text()
                .append(Component.text("    [argument]").color(NamedTextColor.DARK_AQUA))
                .append(Component.space())
                .append(Component.text(" - optional"))
                .build()
        }
    }

    private val rootName: String = parent?.let { "${it.rootName} ${delegate.name}" } ?: delegate.name
    private val rootPermission: String = parent?.let { "${it.rootPermission}.${delegate.name}" } ?: delegate.name
    private val helpComponents by lazy {
        mutableListOf(
            lineMessage,
            Component.empty(),
            requiredMessage,
            optionalMessage,
            Component.empty(),
        ).apply {
            sub.sortedBy {
                it.name
            }.forEach {
                add(it.toComponent())
            }
            add(lineMessage)
        }
    }

    init {
        delegate.withPermission(rootPermission)
    }

    private fun CommandAPICommand.autoPermission() = withPermission("$rootPermission.$name")
    private val sub = mutableListOf(
        CommandAPICommand("help")
            .withAliases("h")
            .withShortDescription("shows help command to player.")
            .autoPermission()
            .executes(this)
    )

    fun command(name: String, block: CommandAPICommand.() -> Unit): CommandModule {
        sub.add(CommandAPICommand(name)
            .autoPermission()
            .apply(block)
        )
        return this
    }

    fun commandModule(name: String, block: CommandAPICommand.() -> Unit) = CommandModule(this, CommandAPICommand(name).apply(block))

    fun build(): CommandAPICommand = delegate
        .withSubcommands(*sub.toTypedArray())
        .executes(this)


    override fun run(info: ExecutionInfo<CommandSender, BukkitCommandSender<out CommandSender>>) {
        val audience = info.sender().audience()
        helpComponents.forEach {
            audience.info(it)
        }
    }

    private fun CommandAPICommand.toComponent() = Component.text()
        .append(Component.text().content("/$rootName").color(NamedTextColor.YELLOW))
        .append(Component.space())
        .append(Component.text(name))
        .append(arguments.map {
            Component.space().append(it.toComponent())
        })
        .append(Component.text().content(" - ").color(NamedTextColor.DARK_GRAY))
        .append(Component.text(shortDescription).color(NamedTextColor.GRAY))
        .hoverEvent(HoverEvent.showText(Component.text()
            .append {
                if (aliases.isNotEmpty()) Component.text()
                    .append(Component.text("Aliases:").color(NamedTextColor.DARK_AQUA))
                    .append(Component.newline())
                    .append(aliases.map {
                        Component.text(it).append(Component.newline())
                    })
                    .append(Component.newline())
                    .build()
                else Component.empty()
            }
            .append(Component.text("Click to suggest command."))
        ))
        .clickEvent(ClickEvent.suggestCommand("/$rootName $name"))
        .build()

    private fun Argument<*>.toComponent() = Component.text()
        .content(if (isOptional) "[$nodeName]" else "<$nodeName>")
        .color(if (isOptional) NamedTextColor.DARK_AQUA else NamedTextColor.RED)
        .hoverEvent(HoverEvent.showText(Component.text(argumentType.name)))
}