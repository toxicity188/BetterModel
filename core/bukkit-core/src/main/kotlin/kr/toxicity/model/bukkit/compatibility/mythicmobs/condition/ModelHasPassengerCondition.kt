/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.bukkit.compatibility.mythicmobs.condition

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.conditions.IEntityCondition
import io.lumine.mythic.api.skills.conditions.ISkillMetaCondition
import kr.toxicity.model.bukkit.compatibility.mythicmobs.*
import kr.toxicity.model.util.boneName

class ModelHasPassengerCondition(mlc: MythicLineConfig) : IEntityCondition {

    private val seat = mlc.toPlaceholderStringList(MM_SEAT) {
        it.map { s -> s.boneName.name }.toSet()
    }

    override fun check(p0: AbstractEntity): Boolean {
        val args = p0.toPlaceholderArgs()
        val set = seat(args)
        return p0.toRegistry()?.let {
            if (set.isEmpty()) it.hasPassenger() else set.any { seat ->
                it.mountedHitBox().values.any { box ->
                    box.hitBox().positionSource().name().name == seat
                }
            }
        } == true
    }
}

internal class HasModelCondition(config: MythicLineConfig) : IEntityCondition {
    private val model = config.megModelId
    private val state = config.toPlaceholderString(arrayOf("s", "state"))

    override fun check(target: AbstractEntity): Boolean {
        val args = target.toPlaceholderArgs()
        val animation = state(args)?.lowercase()
        return target.selectedTrackers(model(args)).any { tracker ->
            animation == null || tracker.bones().any { it.runningAnimation()?.name() == animation }
        }
    }
}

internal class PartVisibilityCondition(config: MythicLineConfig) : IEntityCondition {
    private val model = config.megModelId
    private val part = config.toPlaceholderString(arrayOf("p", "pid", "part", "partid"), "")
    private val visible = config.getBoolean(arrayOf("v", "visible", "visibility"), true)
    private val exact = config.getBoolean(arrayOf("em", "exact", "match", "exactmatch"), true)
    private val child = config.getBoolean(arrayOf("c", "ch", "child"), false)

    override fun check(target: AbstractEntity): Boolean {
        val args = target.toPlaceholderArgs()
        val id = part(args).orEmpty().lowercase()
        var matched = false
        for (tracker in target.selectedTrackers(model(args))) for (bone in tracker.bones()) {
            val display = bone.display ?: continue
            if (generateSequence(bone) { if (child) it.parent else null }.none {
                val name = it.name().name
                id.isEmpty() || (if (exact) name == id else name.contains(id))
            }) continue
            matched = true
            if (display.invisible() == visible) return false
        }
        return matched
    }
}

internal class HitboxCondition(config: MythicLineConfig, argument: String?) : ISkillMetaCondition {
    private val name = config.toPlaceholderString(arrayOf("hitbox", "hb", "h", "ob"), argument)

    override fun check(meta: SkillMetadata) = meta.variables.has("hitbox") &&
        meta.variables.getString("hitbox")?.equals(name(meta.toPlaceholderArgs()), ignoreCase = true) == true
}

internal class ModelPassengersCondition(config: MythicLineConfig) : IEntityCondition {
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
