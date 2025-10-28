/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.command

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.SuggestionInfo
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.IntegerArgument
import dev.jorel.commandapi.commandsenders.BukkitCommandSender
import dev.jorel.commandapi.executors.CommandArguments
import dev.jorel.commandapi.executors.CommandExecutionInfo
import dev.jorel.commandapi.executors.ExecutionInfo
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.data.renderer.ModelRenderer
import kr.toxicity.model.util.*
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor.*
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.command.CommandSender

fun commandModule(name: String, block: CommandAPICommand.() -> Unit) = CommandModule(null, CommandAPICommand(name).apply(block))

fun Argument<*>.suggest(collections: Collection<String>): Argument<*> = replaceSuggestions(ArgumentSuggestions.strings(collections))
fun Argument<*>.suggest(block: (SuggestionInfo<CommandSender>) -> Collection<String>): Argument<*> = replaceSuggestions(ArgumentSuggestions.stringCollection(block))
fun Argument<*>.suggestNullable(collections: Collection<String>?): Argument<*> = suggest(collections ?: emptySet())
fun Argument<*>.suggestNullable(block: (SuggestionInfo<CommandSender>) -> Collection<String>?): Argument<*> = suggest { block(it) ?: emptySet() }

inline fun <reified T : Any> CommandArguments.map(name: String, ifNull: () -> T) = get(name) as? T ?: ifNull()
inline fun <reified T : Any> CommandArguments.map(name: String, ifNull: T) = get(name) as? T ?: ifNull
inline fun <reified T : Any> CommandArguments.map(name: String) = get(name) as T
inline fun <reified T : Any> CommandArguments.any(name: String, block: (T) -> Boolean): Boolean {
    var success = false
    mapNullable<Collection<T>>(name)?.forEach {
        if (block(it)) success = true
    }
    return success
}
inline fun <reified T : Any> CommandArguments.mapNullable(name: String) = get(name) as? T
inline fun <reified T : Any> CommandArguments.mapString(name: String, mapper: (String) -> T) = map<String>(name).let(mapper)
inline fun <reified T : Any> CommandArguments.mapNullableString(name: String, mapper: (String) -> T?) = mapNullable<String>(name)?.let(mapper)
inline fun CommandArguments.mapToModel(name: String, ifNotFound: (String) -> ModelRenderer) = mapString(name) { BetterModel.modelOrNull(it) ?: ifNotFound(it) }
inline fun CommandArguments.mapToLimb(name: String, ifNotFound: (String) -> ModelRenderer) = mapString(name) { BetterModel.limbOrNull(it) ?: ifNotFound(it) }

class CommandModule(
    parent: CommandModule?,
    private val delegate: CommandAPICommand
) : CommandExecutionInfo {
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

        val CommandAPICommand.shortName: String get() = if (aliases.isNotEmpty()) aliases.first() else name

        fun String.toTypeName() = lowercase().replace('_', ' ')
    }

    private val rootName: String = parent?.let { "${it.rootName} ${delegate.name}" } ?: delegate.shortName
    private val rootPermission: String = parent?.let { "${it.rootPermission}.${delegate.name}" } ?: delegate.name

    private val rootNameComponent = "/$rootName".toComponent(YELLOW)
    private val sub = mutableMapOf<String, CommandAPICommand>()
    private val maxPage get() = sub.size / PAGE_SPLIT_INDEX + 1
    private val helpComponentRange get() = 1..maxPage
    private val helpComponents by lazy {
        helpComponentRange.map { index ->
            (if (index == 1) fullPrefix else prefix).toMutableList()
                .also { list ->
                    sub.values.toList().subList(PAGE_SPLIT_INDEX * (index - 1), (PAGE_SPLIT_INDEX * index).coerceAtMost(sub.size)).forEach {
                        list += it.toComponent()
                    }
                    list += "/$rootName [help] [page] - help command.".toComponent(LIGHT_PURPLE)
                    list += emptyComponentOf()
                    list += "---------< Page $index / $maxPage >---------".toComponent(GRAY)
                }.toTypedArray()
        }
    }
    private val pageArgs get() = IntegerArgument("page")

    init {
        delegate.withPermission(rootPermission)
        command("help") {
            withAliases("h")
            withOptionalArguments(pageArgs.suggest { helpComponentRange.map(Any::toString) })
            withShortDescription("shows help command to player.")
            executes(this@CommandModule)
        }
    }

    fun command(name: String, block: CommandAPICommand.() -> Unit): CommandModule {
        sub[name] = CommandAPICommand(name)
            .withPermission("$rootPermission.$name")
            .apply(block)
        return this
    }

    fun commandModule(name: String, block: CommandAPICommand.() -> Unit) = CommandModule(this, CommandAPICommand(name).apply(block))

    fun build(): CommandAPICommand = delegate
        .withOptionalArguments(pageArgs)
        .withSubcommands(*sub.values.toTypedArray())
        .executes(this)


    override fun run(info: ExecutionInfo<CommandSender, BukkitCommandSender<out CommandSender>>) {
        val page = (info.args().mapNullable<Int>("page") ?: 1)
            .coerceAtLeast(1)
            .coerceAtMost(sub.size / PAGE_SPLIT_INDEX + 1)
        info.sender().audience().info(*helpComponents[page - 1])
    }

    private fun CommandAPICommand.toComponent() = componentOf {
        append(rootNameComponent)
        append(spaceComponentOf())
        append(name.toComponent())
        append(arguments.map {
            spaceComponentOf().append(it.toComponent())
        })
        append(" - ".toComponent(DARK_GRAY))
        append(shortDescription.toComponent(GRAY))
        hoverEvent(componentOf(
            if (aliases.isNotEmpty()) componentOf(
                "Aliases:".toComponent(DARK_AQUA),
                lineComponentOf(),
                componentWithLineOf(*aliases.map(String::toComponent).toTypedArray())
            ) else emptyComponentOf(),
            lineComponentOf(),
            lineComponentOf(),
            "Click to suggest command.".toComponent()
        ).toHoverEvent())
        clickEvent(ClickEvent.suggestCommand("/$rootName $shortName"))
    }

    private fun Argument<*>.toComponent() = componentOf {
        content(if (isOptional) "[${nodeName.toTypeName()}]" else "<${nodeName.toTypeName()}>")
        color(if (isOptional) DARK_AQUA else RED)
        hoverEvent(argumentType.name.toTypeName().toComponent().toHoverEvent())
    }
}