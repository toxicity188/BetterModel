package kr.toxicity.model.purpur

import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.event.CreateDummyTrackerEvent
import kr.toxicity.model.api.event.CreateEntityTrackerEvent
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

object PurpurHook {
    fun start() {
        val plugin = BetterModel.inst()
        val config = plugin.configManager()
        plugin.logger().info(
            "BetterModel is currently running in Purpur.",
            "Some Purpur features will be enabled."
        )
        Bukkit.getPluginManager().registerEvents(object : Listener {
            @EventHandler
            fun CreateEntityTrackerEvent.create() {
                tracker().instance.viewFilter {
                    !config.usePurpurAfk() || !it.isAfk
                }
            }
            @EventHandler
            fun CreateDummyTrackerEvent.create() {
                tracker().instance.viewFilter {
                    !config.usePurpurAfk() || !it.isAfk
                }
            }
        }, plugin as Plugin)
    }
}