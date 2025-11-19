/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.command

import kr.toxicity.model.util.*
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor.*
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.command.CommandSender
import org.incendo.cloud.Command
import org.incendo.cloud.CommandManager
import org.incendo.cloud.bukkit.BukkitCommandMeta
import org.incendo.cloud.description.Description
import org.incendo.cloud.parser.standard.IntegerParser

class CommandBuilder(
    val parent: CommandBuilder?,
    val manager: CommandManager<CommandSender>,
    val info: Info
) : CommandLike {

    private companion object {
        const val PAGE_SPLIT_INDEX = 6

        val prefix = listOf(
            emptyComponentOf(),
            "------ BetterModel ${PLUGIN.semver()} ------".toComponent(GRAY),
            emptyComponentOf()
        )

        val fullPrefix = listOf(
            prefix,
            listOf(
                componentOf {
                    decorate(TextDecoration.BOLD)
                    append(spaceComponentOf())
                    append("[Wiki]".toComponent {
                        color(AQUA)
                        toURLComponent("https://github.com/toxicity188/BetterModel/wiki")
                    })
                    append(spaceComponentOf())
                    append("[Download]".toComponent {
                        color(GREEN)
                        toURLComponent("https://modrinth.com/plugin/bettermodel/versions")
                    })
                    append(spaceComponentOf())
                    append("[Discord]".toComponent {
                        color(BLUE)
                        toURLComponent("https://discord.com/invite/rePyFESDbk")
                    })
                },
                emptyComponentOf(),
                componentOf(
                    "    <arg>".toComponent(RED),
                    spaceComponentOf(),
                    " - required".toComponent()
                ),
                componentOf(
                    "    [arg]".toComponent(DARK_AQUA),
                    spaceComponentOf(),
                    " - optional".toComponent()
                ),
                emptyComponentOf()
            )
        ).flatten()

        fun TextComponent.Builder.toURLComponent(url: String) = hoverEvent(componentOf(
            url.toComponent(DARK_AQUA),
            lineComponentOf(),
            lineComponentOf(),
            "Click to open link.".toComponent()
        ).toHoverEvent()).clickEvent(ClickEvent.openUrl(url))
    }

    private val root: CommandBuilder = parent?.root ?: this
    private val suggest: String = parent?.let { "${it.suggest} ${info.name}" } ?: info.simpleName
    private val permission: String = parent?.let { "${it.permission} ${info.name}" } ?: info.name
    private val children = mutableListOf<CommandLike>()
    private val helpCommand by lazy {
        val maxPage = children.size / PAGE_SPLIT_INDEX + 1
        val helpComponents = (1..maxPage).map { index ->
            (if (index == 1) fullPrefix else prefix).toMutableList()
                .also { list ->
                    children.subList(PAGE_SPLIT_INDEX * (index - 1), (PAGE_SPLIT_INDEX * index).coerceAtMost(children.size)).forEach {
                        list += it.toComponent()
                    }
                    list += "/$suggest [help] [page] - help command.".toComponent(LIGHT_PURPLE)
                    list += emptyComponentOf()
                    list += "---------< Page $index / $maxPage >---------".toComponent(GRAY)
                }.toTypedArray()
        }
        val builder = createBuilder()
            .permission("$permission.help")
            .handler { ctx ->
                val page = ctx.getOrDefault("page", 1)
                    .coerceAtLeast(1)
                    .coerceAtMost(maxPage)
                ctx.sender().audience().info(*helpComponents[page - 1])
            }
        listOf(
            builder
                .optional("page", IntegerParser.integerParser(1, maxPage))
                .build(),
            builder.literal("help", "h")
                .optional("page", IntegerParser.integerParser(1, maxPage))
                .build()
        )
    }

    fun create(
        name: String,
        description: String,
        vararg aliases: String,
        builder: Command.Builder<CommandSender>.() -> Command.Builder<out CommandSender>
    ) {
        children += CommandLike.Cloud(createBuilder()
            .mapInfo(Info(name, Description.description(description), aliases.toList()))
            .run(builder)
            .build())
    }

    data class Info(
        val name: String,
        val description: Description,
        val aliases: List<String>
    ) {
        val simpleName get() = if (aliases.isNotEmpty()) aliases.minBy { it.length } else name
    }

    override fun toComponent(): TextComponent {
        TODO("Not yet implemented")
    }

    override fun build(): List<Command<out CommandSender>> = buildList {
        children.flatMapTo(this) { it.build() }
        addAll(helpCommand)
    }

    private fun Command.Builder<CommandSender>.mapInfo(info: Info) = literal(info.name, *info.aliases.toTypedArray())
        .commandDescription(info.description)
        .permission("$permission.${info.name}")

    private fun createBuilder(): Command.Builder<CommandSender> = parent?.createBuilder()?.mapInfo(info) ?: manager.commandBuilder(
        info.name,
        info.description,
        *info.aliases.toTypedArray()
    ).meta(BukkitCommandMeta.BUKKIT_DESCRIPTION, info.description.textDescription())
}