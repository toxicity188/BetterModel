/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.bukkit.util

import kr.toxicity.model.util.PLATFORM
import org.bukkit.Bukkit
import org.bukkit.event.Listener

fun registerListener(listener: Listener) {
    Bukkit.getPluginManager().registerEvents(listener, PLATFORM)
}
