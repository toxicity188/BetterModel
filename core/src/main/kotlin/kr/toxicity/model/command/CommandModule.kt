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
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.command.CommandSender

fun commandModule(name: String, block: CommandAPICommand.() -> Unit) = CommandModule(null, CommandAPICommand(name).apply(block))

class CommandModule(
    parent: CommandModule?,
    private val delegate: CommandAPICommand
) : CommandExecutionInfo {
    companion object {
        private val upperLineMessage = componentOf("------ BetterModel ${PLUGIN.semver()} ------") {
            color(NamedTextColor.GRAY)
        }
        private val underLineMessage = componentOf("----------------------------------") {
            color(NamedTextColor.GRAY)
        }
        private val requiredMessage = componentOf {
            append(componentOf("    <arg>") {
                color(NamedTextColor.RED)
            })
            append(spaceComponentOf())
            append(componentOf(" - required"))
        }
        private val optionalMessage = componentOf {
            append(componentOf("    [arg]") {
                color(NamedTextColor.DARK_AQUA)
            })
            append(spaceComponentOf())
            append(componentOf(" - optional"))
        }
        private val usefulLinks = componentOf {
            decorate(TextDecoration.BOLD)
            append(spaceComponentOf())
            append(componentOf("[Wiki]") {
                color(NamedTextColor.AQUA)
                toURLComponent("https://github.com/toxicity188/BetterModel/wiki")
            })
            append(spaceComponentOf())
            append(componentOf("[Download]") {
                color(NamedTextColor.GREEN)
                toURLComponent("https://modrinth.com/plugin/bettermodel/versions")
            })
            append(spaceComponentOf())
            append(componentOf("[Discord]") {
                color(NamedTextColor.BLUE)
                toURLComponent("https://discord.com/invite/rePyFESDbk")
            })
        }

        private fun TextComponent.Builder.toURLComponent(url: String) = hoverEvent(HoverEvent.showText(componentOf {
            append(componentOf(url) {
                color(NamedTextColor.AQUA)
            })
            append(lineComponentOf())
            append(lineComponentOf())
            append(componentOf("Click to open link."))
        })).clickEvent(ClickEvent.openUrl(url))

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
        clickEvent(ClickEvent.suggestCommand("/$rootName $shortName"))
    }

    private fun Argument<*>.toComponent() = componentOf {
        content(if (isOptional) "[${nodeName.toTypeName()}]" else "<${nodeName.toTypeName()}>")
        color(if (isOptional) NamedTextColor.DARK_AQUA else NamedTextColor.RED)
        hoverEvent(HoverEvent.showText(Component.text(argumentType.name.toTypeName())))
    }
}