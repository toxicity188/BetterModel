/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.bukkit.compatibility.mythicmobs.targeter

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.targeters.IEntityTargeter
import io.lumine.mythic.bukkit.BukkitAdapter
import kr.toxicity.model.bukkit.compatibility.mythicmobs.megModelId
import kr.toxicity.model.bukkit.compatibility.mythicmobs.toPlaceholderArgs
import kr.toxicity.model.bukkit.compatibility.mythicmobs.toPlaceholderString
import kr.toxicity.model.bukkit.compatibility.mythicmobs.toRegistry
import kr.toxicity.model.bukkit.util.unwarp

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
