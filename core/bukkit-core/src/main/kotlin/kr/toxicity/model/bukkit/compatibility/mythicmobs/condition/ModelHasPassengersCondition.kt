/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.bukkit.compatibility.mythicmobs.condition

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.conditions.IEntityCondition
import kr.toxicity.model.bukkit.compatibility.mythicmobs.*

internal class ModelHasPassengersCondition(config: MythicLineConfig) : IEntityCondition {
    private val model = config.megModelId
    private val seats = config.toPlaceholderString(arrayOf("p", "pbone", "seat"))
    private val mode = config.getString("mode", "AND").uppercase()

    override fun check(target: AbstractEntity): Boolean {
        val registry = target.toRegistry() ?: return false
        val args = target.toPlaceholderArgs()
        val trackers = target.selectedTrackers(model(args))
        val requested = seats(args)?.lowercase()?.takeIf { it.isNotBlank() }?.split(',')
        if (mode != "AND" && mode != "OR") return false
        if (requested == null) return trackers.any { tracker ->
            registry.mountedHitBox().values.any { tracker.bone(it.hitBox().positionSource().name()) === it.hitBox().positionSource() }
        }
        if (mode == "OR") return trackers.any { tracker -> requested.any { name ->
            tracker.bone(name)?.hitBox?.let { box -> !box.mountController().canControl() &&
                registry.mountedHitBox().values.any { it.hitBox().positionSource() === box.positionSource() }
            } == true
        } }
        return trackers.isNotEmpty() && trackers.all { tracker -> requested.all { name ->
            tracker.bone(name)?.hitBox?.let { box -> box.mountController().canControl() ||
                registry.mountedHitBox().values.any { it.hitBox().positionSource() === box.positionSource() }
            } == true
        } }
    }
}
