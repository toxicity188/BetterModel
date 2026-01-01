/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.compatibility.mythicmobs.mechanic

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.INoTargetSkill
import io.lumine.mythic.api.skills.ITargetedEntitySkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import kr.toxicity.model.api.animation.AnimationModifier
import kr.toxicity.model.api.util.function.FloatConstantSupplier
import kr.toxicity.model.compatibility.mythicmobs.*
import org.bukkit.entity.Player

class StateMechanic(mlc: MythicLineConfig) : AbstractSkillMechanic(mlc), INoTargetSkill, ITargetedEntitySkill {

    private val model = mlc.modelPlaceholder
    private val state = mlc.toPlaceholderString(arrayOf("state", "s"))
    private val li = mlc.toPlaceholderInteger(arrayOf("li"), 1)
    private val lo = mlc.toPlaceholderInteger(arrayOf("lo"))
    private val sp = mlc.toNullablePlaceholderFloat(arrayOf("speed", "sp"))
    private val remove = mlc.toPlaceholderBoolean(arrayOf("remove", "r"))

    override fun cast(p0: SkillMetadata): SkillResult {
        return cast(null, p0)
    }

    override fun castAtEntity(p0: SkillMetadata, p1: AbstractEntity): SkillResult {
        return cast(p1.bukkitEntity as? Player, p0)
    }

    private fun cast(player: Player?, meta: SkillMetadata): SkillResult {
        val args = meta.toPlaceholderArgs()
        return meta.toTracker(model(args))?.let {
            val s = state(args) ?: return SkillResult.CONDITION_FAILED
            if (remove(args)) it.stopAnimation(s) else it.animate(s, AnimationModifier.builder()
                .start(li(args))
                .end(lo(args))
                .speed(sp(args)?.let(FloatConstantSupplier::of))
                .player(player)
                .build())
            SkillResult.SUCCESS
        } ?: SkillResult.CONDITION_FAILED
    }
}
