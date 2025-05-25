package kr.toxicity.model.util

import kr.toxicity.model.api.util.EventUtil
import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.bukkit.event.Listener

fun registerListener(listener: Listener) {
    Bukkit.getPluginManager().registerEvents(listener, PLUGIN)
}

fun Event.call(): Boolean = EventUtil.call(this)