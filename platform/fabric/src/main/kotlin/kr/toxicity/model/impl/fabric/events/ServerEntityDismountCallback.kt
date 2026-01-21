/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.impl.fabric.events

import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.world.entity.Entity

fun interface ServerEntityDismountCallback {
    fun onDismount(passenger: Entity, vehicle: Entity): Boolean

    companion object {
        @JvmField
        val EVENT = EventFactory.createArrayBacked(ServerEntityDismountCallback::class.java) { callbacks ->
            { passenger, vehicle ->
                callbacks.all { it.onDismount(passenger, vehicle) }
            }
        }
    }
}
