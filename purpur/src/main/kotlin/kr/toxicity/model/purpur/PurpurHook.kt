/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.purpur

import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.event.CreateTrackerEvent
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

object PurpurHook {
    fun start() {
        val plugin = BetterModel.plugin()
        val config = BetterModel.config()
        plugin.logger().info(
            "BetterModel is currently running in Purpur.",
            "Some Purpur features will be enabled."
        )
        Bukkit.getPluginManager().registerEvents(object : Listener {
            @EventHandler
            fun CreateTrackerEvent.create() {
                tracker().pipeline.viewFilter {
                    !config.usePurpurAfk() || !it.isAfk
                }
            }
        }, plugin as Plugin)
    }
}