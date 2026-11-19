/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.bukkit.compatibility.mythicmobs.mechanic

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.ITargetedEntitySkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import kr.toxicity.model.api.tracker.ModelScaler
import kr.toxicity.model.bukkit.compatibility.mythicmobs.*

internal class ScaleMechanic(config: MythicLineConfig) : AbstractSkillMechanic(config), ITargetedEntitySkill {
    private val model = config.megModelId
    private val size = config.toPlaceholderFloat(arrayOf("s", "scale"), 1F)
    private val hitbox = config.getBoolean(arrayOf("h", "hitbox"), false)

    override fun castAtEntity(meta: SkillMetadata, target: AbstractEntity): SkillResult {
        if (hitbox || target.toRegistry() == null) return SkillResult.CONDITION_FAILED
        val args = toPlaceholderArgs(meta, target)
        val scale = ModelScaler.entity().multiply(size(args))
        target.selectedTrackers(model(args)).forEach { it.scaler(scale) }
        return SkillResult.SUCCESS
    }
}
