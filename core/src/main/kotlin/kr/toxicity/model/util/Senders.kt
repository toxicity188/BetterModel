/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.util

import kr.toxicity.model.BetterModelLibrary
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.command.CommandSender

val ADVENTURE_PLATFORM = if (BetterModelLibrary.ADVENTURE_PLATFORM.isLoaded) BukkitAudiences.create(PLUGIN) else null

val INFO = componentOf(" [!] ") {
    decorate(TextDecoration.BOLD).color(NamedTextColor.GREEN)
}
val WARN = componentOf(" [!] ") {
    decorate(TextDecoration.BOLD).color(NamedTextColor.RED)
}

fun String.toComponent() = componentOf(this).build()
fun String.toComponent(color: TextColor) = componentOf(this).color(color).build()

fun componentOf() = Component.text()
fun spaceComponentOf() = Component.space()
fun emptyComponentOf() = Component.empty()
fun lineComponentOf() = Component.newline()
fun componentOf(content: String) = componentOf().content(content)
fun componentOf(vararg like: ComponentLike) = componentOf {
    append(*like)
}
fun componentWithLineOf(vararg like: ComponentLike) = componentOf {
    like.forEachIndexed { i, l ->
        append(l)
        if (i < like.lastIndex) append(lineComponentOf())
    }
    this
}
inline fun componentOf(content: String, builder: TextComponent.Builder.() -> TextComponent.Builder) = componentOf {
    content(content).let(builder)
}
inline fun componentOf(builder: TextComponent.Builder.() -> TextComponent.Builder) = componentOf().let(builder).build()

fun CommandSender.audience() = ADVENTURE_PLATFORM?.sender(this) ?: this
fun Audience.info(message: String) = info(componentOf(message))
fun Audience.warn(message: String) = warn(componentOf(message))
fun Audience.infoNotNull(vararg messages: ComponentLike?) = info(*messages.filterNotNull().ifEmpty {
    return
}.toTypedArray())
fun Audience.info(vararg messages: ComponentLike) = sendMessage(componentWithLineOf(*messages.map { componentOf(INFO, it) }.toTypedArray()))
fun Audience.warn(vararg messages: ComponentLike) = sendMessage(componentWithLineOf(*messages.map { componentOf(WARN, it) }.toTypedArray()))
fun Audience.info(message: ComponentLike) = sendMessage(componentOf(INFO, message))
fun Audience.warn(message: ComponentLike) = sendMessage(componentOf(WARN, message))