/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.util

import kr.toxicity.model.api.util.EventUtil
import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.bukkit.event.Listener

fun registerListener(listener: Listener) {
    Bukkit.getPluginManager().registerEvents(listener, PLUGIN)
}

fun Event.call(): Boolean = EventUtil.call(this)