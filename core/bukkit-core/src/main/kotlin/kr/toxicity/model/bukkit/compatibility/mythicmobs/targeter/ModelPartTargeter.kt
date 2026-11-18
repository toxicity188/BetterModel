/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.bukkit.compatibility.mythicmobs.targeter

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.adapters.AbstractLocation
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.targeters.IEntityTargeter
import io.lumine.mythic.api.skills.targeters.ILocationTargeter
import io.lumine.mythic.bukkit.BukkitAdapter
import kr.toxicity.model.bukkit.compatibility.mythicmobs.*
import kr.toxicity.model.bukkit.util.unwarp
import kr.toxicity.model.util.boneName

class ModelPartTargeter(mlc: MythicLineConfig) : ILocationTargeter {

    private val model = mlc.modelPlaceholder
    private val part = mlc.toPlaceholderString(MM_PART_ID) {
        it?.boneName?.name
    }

    override fun getLocations(p0: SkillMetadata): Collection<AbstractLocation> {
        val args = p0.toPlaceholderArgs()
        return p0.toTracker(model(args))?.bone(part(args) ?: return emptyList())?.hitBoxPosition()?.let {
            listOf(p0.caster.entity.location.add(
                it.x.toDouble(),
                it.y.toDouble(),
                it.z.toDouble()
            ))
        } ?: emptyList()
    }
}

internal class ModelPassengersTargeter(config: MythicLineConfig) : IEntityTargeter {
    private val model = config.megModelId
    private val seat = config.toPlaceholderString(arrayOf("p", "pbone", "seat"))

    override fun getEntities(meta: SkillMetadata): Collection<AbstractEntity> {
        val args = meta.toPlaceholderArgs()
        val seats = seat(args)?.lowercase()?.split(',')?.toSet() ?: return emptySet()
        val registry = meta.toRegistry() ?: return emptySet()
        val tracker = model(args)?.let(registry::tracker)
        return registry.mountedHitBox().values.mapNotNullTo(mutableSetOf()) {
            val bone = it.hitBox().positionSource()
            if (bone.name().name in seats && (tracker == null || tracker.bone(bone.name()) === bone))
                BukkitAdapter.adapt(it.entity().unwarp()) else null
        }
    }
}
