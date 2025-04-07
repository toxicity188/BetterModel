package kr.toxicity.model.purpur

import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.event.CreateTrackerEvent
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
            fun CreateTrackerEvent.create() {
                tracker.instance.viewFilter {
                    !config.usePurpurAfk() || !it.isAfk
                }
            }
        }, plugin as Plugin)
    }
}