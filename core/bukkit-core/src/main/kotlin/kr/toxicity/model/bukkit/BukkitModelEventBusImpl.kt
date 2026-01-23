/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.bukkit

import kr.toxicity.model.BetterModelEventBusImpl
import kr.toxicity.model.api.BetterModelEventBus
import kr.toxicity.model.api.bukkit.BukkitModelEventBus
import kr.toxicity.model.api.bukkit.event.BetterModelBukkitEvent
import kr.toxicity.model.api.event.CancellableEvent
import org.bukkit.Bukkit

class BukkitModelEventBusImpl : BukkitModelEventBus, BetterModelEventBus by BetterModelEventBusImpl({ eventClass, supplier ->
    BetterModelBukkitEvent(eventClass, supplier).apply {
        Bukkit.getPluginManager().callEvent(this)
    }.source()?.let { event ->
        if (event !is CancellableEvent || !event.isCancelled()) BetterModelEventBus.Result.SUCCESS else BetterModelEventBus.Result.FAIL
    } ?: BetterModelEventBus.Result.NO_EVENT_HANDLER
})
