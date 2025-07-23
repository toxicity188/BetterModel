package kr.toxicity.model.command

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.commandsenders.BukkitCommandSender
import dev.jorel.commandapi.executors.CommandExecutionInfo
import dev.jorel.commandapi.executors.ExecutionInfo
import kr.toxicity.model.util.audience
import kr.toxicity.model.util.componentOf
import kr.toxicity.model.util.componentWithLineOf
import kr.toxicity.model.util.emptyComponentOf
import kr.toxicity.model.util.info
import kr.toxicity.model.util.lineComponentOf
import kr.toxicity.model.util.spaceComponentOf
import kr.toxicity.model.util.toComponent
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
            componentOf("----------------------------------------") {
                color(NamedTextColor.GRAY)
            }
        }
        private val requiredMessage by lazy {
            componentOf {
                append(componentOf("    <argument>") {
                    color(NamedTextColor.RED)
                })
                append(spaceComponentOf())
                append(componentOf(" - required"))
            }
        }
        private val optionalMessage by lazy {
            componentOf {
                append(componentOf("    [argument]") {
                    color(NamedTextColor.DARK_AQUA)
                })
                append(spaceComponentOf())
                append(componentOf(" - optional"))
            }
        }
    }

    private val rootName: String = parent?.let { "${it.rootName} ${delegate.name}" } ?: delegate.name
    private val rootPermission: String = parent?.let { "${it.rootPermission}.${delegate.name}" } ?: delegate.name
    private val helpComponents by lazy {
        mutableListOf(
            lineMessage,
            emptyComponentOf(),
            requiredMessage,
            optionalMessage,
            emptyComponentOf(),
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
        helpComponents.forEach(audience::info)
    }

    private fun CommandAPICommand.toComponent() = componentOf {
        append(componentOf("/$rootName") {
            color(NamedTextColor.YELLOW)
        })
        append(spaceComponentOf())
        append(name.toComponent())
        append(arguments.map {
            spaceComponentOf().append(it.toComponent())
        })
        append(componentOf(" - ") {
            color(NamedTextColor.DARK_GRAY)
        })
        append(componentOf(shortDescription) {
            color(NamedTextColor.GRAY)
        })
        hoverEvent(
            HoverEvent.showText(componentOf {
                append(if (aliases.isNotEmpty()) componentOf {
                    append(componentOf("Aliases:") {
                        color(NamedTextColor.DARK_AQUA)
                    })
                    append(lineComponentOf())
                    append(componentWithLineOf(*aliases.map(String::toComponent).toTypedArray()))
                } else emptyComponentOf())
                append(lineComponentOf())
                append(lineComponentOf())
                append("Click to suggest command.".toComponent())
            }
        ))
        clickEvent(ClickEvent.suggestCommand("/$rootName $name"))
    }

    private fun Argument<*>.toComponent() = componentOf {
        content(if (isOptional) "[$nodeName]" else "<$nodeName>")
        color(if (isOptional) NamedTextColor.DARK_AQUA else NamedTextColor.RED)
        hoverEvent(HoverEvent.showText(Component.text(argumentType.name)))
    }
}