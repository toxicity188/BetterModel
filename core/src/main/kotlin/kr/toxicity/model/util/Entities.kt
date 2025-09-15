/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.util

import kr.toxicity.model.api.nms.NMSVersion
import kr.toxicity.model.api.tracker.EntityTrackerRegistry
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.entity.Entity

val ATTRIBUTE_SCALE by lazy {
    Registry.ATTRIBUTE.get(NamespacedKey.minecraft(if (PLUGIN.nms().version() >= NMSVersion.V1_21_R2) "scale" else "generic.scale"))!!
}

fun Entity.toTracker(model: String?) = toRegistry()?.tracker(model)
fun Entity.toRegistry() = EntityTrackerRegistry.registry(uniqueId)