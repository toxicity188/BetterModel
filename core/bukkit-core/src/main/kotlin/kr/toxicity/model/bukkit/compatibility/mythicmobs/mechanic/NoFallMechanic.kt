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
import kr.toxicity.model.bukkit.compatibility.mythicmobs.toRegistry

internal class NoFallMechanic(config: MythicLineConfig) : AbstractSkillMechanic(config), ITargetedEntitySkill {
    init {
        isAsyncSafe = false
    }

    override fun castAtEntity(meta: SkillMetadata, target: AbstractEntity): SkillResult {
        if (target.toRegistry() != null) target.bukkitEntity.fallDistance = 0F
        return SkillResult.SUCCESS
    }
}
