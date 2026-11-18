/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.bukkit.compatibility.mythicmobs.mechanic

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.INoTargetSkill
import io.lumine.mythic.api.skills.ITargetedEntitySkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.tracker.ModelScaler
import kr.toxicity.model.api.tracker.TrackerModifier
import kr.toxicity.model.bukkit.compatibility.mythicmobs.*
import kr.toxicity.model.bukkit.util.wrap

class ModelMechanic(mlc: MythicLineConfig) : AbstractSkillMechanic(mlc), INoTargetSkill {

    private val mid = mlc.modelPlaceholder
    private val s = mlc.toPlaceholderFloat(arrayOf("scale", "s"), 1F)
    private val st = mlc.toPlaceholderBoolean(arrayOf("sight-trace", "st"), true)
    private val da = mlc.toPlaceholderBoolean(arrayOf("damageanimation", "da", "animation"), false)
    private val dt = mlc.toPlaceholderBoolean(arrayOf("damagetint", "tint", "dt"), true)
    private val r = mlc.toPlaceholderBoolean(arrayOf("remove", "r"), false)

    override fun cast(p0: SkillMetadata): SkillResult {
        val args = p0.toPlaceholderArgs()
        val e = p0.caster.entity.bukkitEntity
        return if (r(args)) {
            if (mid(args)?.let {
                BetterModel.registryOrNull(e.uniqueId)?.remove(it)
            } == true) SkillResult.SUCCESS else SkillResult.CONDITION_FAILED
        } else {
            BetterModel.modelOrNull(mid(args) ?: return SkillResult.CONDITION_FAILED)?.let {
                it.create(e.wrap(), TrackerModifier(
                    st(args),
                    da(args),
                    dt(args)
                )) { t ->
                    t.scaler(ModelScaler.entity().multiply(s(args)))
                }
                SkillResult.SUCCESS
            } ?: SkillResult.CONDITION_FAILED
        }
    }
}

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

internal class NoFallMechanic(config: MythicLineConfig) : AbstractSkillMechanic(config), ITargetedEntitySkill {
    init {
        isAsyncSafe = false
    }

    override fun castAtEntity(meta: SkillMetadata, target: AbstractEntity): SkillResult {
        if (target.toRegistry() != null) target.bukkitEntity.fallDistance = 0F
        return SkillResult.SUCCESS
    }
}
