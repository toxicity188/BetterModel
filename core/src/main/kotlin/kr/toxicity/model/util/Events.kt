package kr.toxicity.model.util

import org.bukkit.Bukkit
import org.bukkit.event.Listener

fun registerListener(listener: Listener) {
    Bukkit.getPluginManager().registerEvents(listener, PLUGIN)
}