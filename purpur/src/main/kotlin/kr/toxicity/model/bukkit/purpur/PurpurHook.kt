/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.bukkit.purpur

import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.bukkit.platform.BukkitPlayer
import kr.toxicity.model.api.event.CreateDummyTrackerEvent
import kr.toxicity.model.api.event.CreateEntityTrackerEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

object PurpurHook {
    fun start() {
        val plugin = BetterModel.platform()
        val config = BetterModel.config()
        plugin.logger().info(
            Component.text("BetterModel is currently running in Purpur.").color(NamedTextColor.LIGHT_PURPLE),
            Component.text("Some Purpur features will be enabled.").color(NamedTextColor.LIGHT_PURPLE)
        )
        BetterModel.eventBus().subscribe(CreateDummyTrackerEvent::class.java) { event ->
            event.tracker().pipeline.viewFilter {
                !config.usePurpurAfk() || !(it as BukkitPlayer).source().isAfk
            }
        }
        BetterModel.eventBus().subscribe(CreateEntityTrackerEvent::class.java) { event ->
            event.tracker().pipeline.viewFilter {
                !config.usePurpurAfk() || !(it as BukkitPlayer).source().isAfk
            }
        }
    }
}
