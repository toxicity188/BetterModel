/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
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
import org.bukkit.entity.Player

class PairModelMechanic(mlc: MythicLineConfig) : AbstractSkillMechanic(mlc), ITargetedEntitySkill {

    private val model = mlc.modelPlaceholder
    private val remove = mlc.toPlaceholderBoolean(arrayOf("remove", "r"), false)

    override fun castAtEntity(p0: SkillMetadata, p1: AbstractEntity): SkillResult {
        val target = p1.bukkitEntity as? Player ?: return SkillResult.CONDITION_FAILED
        val args = toPlaceholderArgs(p0, p1)
        p0.toTracker(model(args))?.let {
            if (remove(args)) {
                it.show(target)
            } else {
                it.hide(target)
            }
        }
        return SkillResult.SUCCESS
    }
}