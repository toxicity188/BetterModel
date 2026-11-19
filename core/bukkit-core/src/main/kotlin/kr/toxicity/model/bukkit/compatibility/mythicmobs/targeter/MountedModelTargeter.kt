/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.bukkit.compatibility.mythicmobs.targeter

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.targeters.IEntityTargeter
import io.lumine.mythic.bukkit.BukkitAdapter
import kr.toxicity.model.bukkit.compatibility.mythicmobs.modelVehicle
import kr.toxicity.model.bukkit.util.unwarp

internal class MountedModelTargeter : IEntityTargeter {
    override fun getEntities(meta: SkillMetadata): Collection<AbstractEntity> {
        return listOfNotNull(meta.caster.entity.bukkitEntity.modelVehicle?.source()?.unwarp()?.let(BukkitAdapter::adapt))
    }
}
