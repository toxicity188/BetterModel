/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.command

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.commandsenders.BukkitCommandSender
import dev.jorel.commandapi.executors.CommandExecutionInfo
import dev.jorel.commandapi.executors.ExecutionInfo
import kr.toxicity.model.util.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor.*
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.command.CommandSender

fun commandModule(name: String, block: CommandAPICommand.() -> Unit) = CommandModule(null, CommandAPICommand(name).apply(block))

class CommandModule(
    parent: CommandModule?,
    private val delegate: CommandAPICommand
) : CommandExecutionInfo {
    companion object {
        private val upperLineMessage = componentOf("------ BetterModel ${PLUGIN.semver()} ------") {
            color(GRAY)
        }
        private val underLineMessage = componentOf("----------------------------------") {
            color(GRAY)
        }
        private val requiredMessage = componentOf(
            "    <arg>".toComponent(RED),
            spaceComponentOf(),
            " - required".toComponent()
        )
        private val optionalMessage = componentOf(
            "    [arg]".toComponent(DARK_AQUA),
            spaceComponentOf(),
            " - optional".toComponent()
        )
        private val usefulLinks = componentOf {
            decorate(TextDecoration.BOLD)
            append(spaceComponentOf())
            append(componentOf("[Wiki]") {
                color(AQUA)
                toURLComponent("https://github.com/toxicity188/BetterModel/wiki")
            })
            append(spaceComponentOf())
            append(componentOf("[Download]") {
                color(GREEN)
                toURLComponent("https://modrinth.com/plugin/bettermodel/versions")
            })
            append(spaceComponentOf())
            append(componentOf("[Discord]") {
                color(BLUE)
                toURLComponent("https://discord.com/invite/rePyFESDbk")
            })
        }

        private fun TextComponent.Builder.toURLComponent(url: String) = hoverEvent(HoverEvent.showText(componentOf(
            url.toComponent(DARK_AQUA),
            lineComponentOf(),
            lineComponentOf(),
            "Click to open link.".toComponent()
        ))).clickEvent(ClickEvent.openUrl(url))

        private val CommandAPICommand.shortName get() = if (aliases.isNotEmpty()) aliases.first() else name

        private fun String.toTypeName() = lowercase().replace('_', ' ')
    }

    private val rootName: String = parent?.let { "${it.rootName} ${delegate.name}" } ?: delegate.shortName
    private val rootPermission: String = parent?.let { "${it.rootPermission}.${delegate.name}" } ?: delegate.name
    private val helpComponents by lazy {
        mutableListOf(
            emptyComponentOf(),
            upperLineMessage,
            emptyComponentOf(),
            usefulLinks,
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
            add(underLineMessage)
            add(emptyComponentOf())
        }.toTypedArray()
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
        info.sender().audience().info(*helpComponents)
    }

    private fun CommandAPICommand.toComponent() = componentOf {
        append("/$rootName".toComponent(YELLOW))
        append(spaceComponentOf())
        append(name.toComponent())
        append(arguments.map {
            spaceComponentOf().append(it.toComponent())
        })
        append(" - ".toComponent(DARK_GRAY))
        append(shortDescription.toComponent(GRAY))
        hoverEvent(
            HoverEvent.showText(componentOf(
                if (aliases.isNotEmpty()) componentOf(
                    "Aliases:".toComponent(DARK_AQUA),
                    lineComponentOf(),
                    componentWithLineOf(*aliases.map(String::toComponent).toTypedArray())
                ) else emptyComponentOf(),
                lineComponentOf(),
                lineComponentOf(),
                "Click to suggest command.".toComponent()
            )
        ))
        clickEvent(ClickEvent.suggestCommand("/$rootName $shortName"))
    }

    private fun Argument<*>.toComponent() = componentOf {
        content(if (isOptional) "[${nodeName.toTypeName()}]" else "<${nodeName.toTypeName()}>")
        color(if (isOptional) DARK_AQUA else RED)
        hoverEvent(HoverEvent.showText(Component.text(argumentType.name.toTypeName())))
    }
}