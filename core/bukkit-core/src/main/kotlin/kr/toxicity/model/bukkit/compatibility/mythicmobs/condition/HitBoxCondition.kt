/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.bukkit.compatibility.mythicmobs.condition

import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.conditions.ISkillMetaCondition
import kr.toxicity.model.bukkit.compatibility.mythicmobs.toPlaceholderArgs
import kr.toxicity.model.bukkit.compatibility.mythicmobs.toPlaceholderString

internal class HitBoxCondition(config: MythicLineConfig, argument: String?) : ISkillMetaCondition {
    private val name = config.toPlaceholderString(arrayOf("hitbox", "hb", "h", "ob"), argument)

    override fun check(meta: SkillMetadata) = meta.variables.has("hitbox") &&
        meta.variables.getString("hitbox")?.equals(name(meta.toPlaceholderArgs()), ignoreCase = true) == true
}
