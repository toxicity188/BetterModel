/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.command

import kr.toxicity.model.util.componentOf
import kr.toxicity.model.util.componentWithLineOf
import kr.toxicity.model.util.lineComponentOf
import kr.toxicity.model.util.spaceComponentOf
import kr.toxicity.model.util.toComponent
import kr.toxicity.model.util.toHoverEvent
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor.DARK_AQUA
import net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY
import net.kyori.adventure.text.format.NamedTextColor.GRAY
import net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE
import net.kyori.adventure.text.format.NamedTextColor.RED
import net.kyori.adventure.text.format.NamedTextColor.YELLOW
import org.bukkit.command.CommandSender
import org.incendo.cloud.Command
import org.incendo.cloud.component.CommandComponent
import org.incendo.cloud.component.CommandComponent.ComponentType.FLAG
import org.incendo.cloud.component.CommandComponent.ComponentType.LITERAL
import org.incendo.cloud.component.CommandComponent.ComponentType.OPTIONAL_VARIABLE
import org.incendo.cloud.component.CommandComponent.ComponentType.REQUIRED_VARIABLE
import kotlin.collections.map

interface CommandLike {

    fun toComponent(): TextComponent

    fun build(): List<Command<out CommandSender>>

    data class Cloud(
        private val command: Command<out CommandSender>
    ) : CommandLike {

        override fun toComponent(): TextComponent = command.toComponent()

        private fun Command<out CommandSender>.toComponent() = componentOf {
            append("/".toComponent())
            components().forEachIndexed { i, comp ->
                append(comp.toComponent(i == 0))
                if (i < components().size) append(spaceComponentOf())
            }
            append(" - ".toComponent(DARK_GRAY))
            append(commandDescription().description().textDescription().toComponent(GRAY))
            hoverEvent(componentOf(
                "Permission:".toComponent(DARK_AQUA),
                lineComponentOf(),
                commandPermission().permissionString().toComponent(),
                lineComponentOf(),
                lineComponentOf(),
                "Click to suggest command.".toComponent()
            ).toHoverEvent())
            clickEvent(ClickEvent.suggestCommand("/" + components().filter {
                it.type() == LITERAL
            }.joinToString(" ") {
                it.name()
            }))
        }

        private fun CommandComponent<out CommandSender>.toComponent(root: Boolean): TextComponent = componentOf {
            val n = if (root) aliases().minBy { it.length } else name()
            when (type()) {
                LITERAL -> content(n).color(YELLOW)
                REQUIRED_VARIABLE -> content("<$n>").color(RED)
                OPTIONAL_VARIABLE -> content("[$n]").color(DARK_AQUA)
                FLAG -> content("-$n").color(LIGHT_PURPLE)
            }
            hoverEvent(componentOf {
                if (aliases().isNotEmpty()) {
                    append(componentOf(
                        "Aliases:".toComponent(DARK_AQUA),
                        lineComponentOf(),
                        componentWithLineOf(*aliases().map(String::toComponent).toTypedArray()),
                        lineComponentOf(),
                        lineComponentOf()
                    ))
                }
                append("Click to suggest command.".toComponent())
            }.toHoverEvent())
        }

        override fun build(): List<Command<out CommandSender>> = listOf(command)
    }
}