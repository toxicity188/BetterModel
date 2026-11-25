/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.bukkit.compatibility.mythicmobs.condition

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.skills.conditions.IEntityCondition
import kr.toxicity.model.bukkit.compatibility.mythicmobs.drivesModel

internal class DrivingModelCondition : IEntityCondition {
    override fun check(target: AbstractEntity): Boolean = target.bukkitEntity.drivesModel
}
