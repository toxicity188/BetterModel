/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.fabric.events

import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.LivingEntity

fun interface ServerMobEffectUnloadCallback {
    fun onUnload(entity: LivingEntity, effect: MobEffectInstance)

    companion object {
        @JvmField
        val EVENT = EventFactory.createArrayBacked(ServerMobEffectUnloadCallback::class.java) { callbacks ->
            { entity, effect ->
                callbacks.forEach { it.onUnload(entity, effect) }
            }
        }
    }
}
