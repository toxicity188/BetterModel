/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.compatibility.mythicmobs.condition

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.conditions.IEntityCondition
import kr.toxicity.model.compatibility.mythicmobs.MM_SEAT
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderArgs
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderStringList
import kr.toxicity.model.compatibility.mythicmobs.toRegistry
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