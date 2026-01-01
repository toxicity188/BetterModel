/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.compatibility.mythicmobs.mechanic

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.ITargetedEntitySkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import kr.toxicity.model.compatibility.mythicmobs.*
import kr.toxicity.model.util.boneName

class DismountModelMechanic(mlc: MythicLineConfig) : AbstractSkillMechanic(mlc), ITargetedEntitySkill {

    private val driver = mlc.toPlaceholderBoolean(arrayOf("driver", "d", "drive"), true)
    private val seat = mlc.toPlaceholderStringList(MM_SEAT) {
        it.map { s -> s.boneName.name }.toSet()
    }

    init {
        isAsyncSafe = false
    }

    override fun castAtEntity(p0: SkillMetadata, p1: AbstractEntity): SkillResult {
        val args = toPlaceholderArgs(p0, p1)
        return p0.toRegistry()?.let { registry ->
            val set = seat(args)
            val d = driver(args)
            registry.mountedHitBox()[p1.bukkitEntity.uniqueId]?.takeIf {
                set.isEmpty() || (set.contains(it.hitBox.positionSource().name().name) && (!d || it.hitBox.mountController().canControl()))
            }?.dismount()
            SkillResult.SUCCESS
        } ?: SkillResult.CONDITION_FAILED
    }
}