/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.compatibility.mythicmobs.mechanic

import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.INoTargetSkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import kr.toxicity.model.api.animation.AnimationModifier
import kr.toxicity.model.api.util.function.FloatConstantSupplier
import kr.toxicity.model.compatibility.mythicmobs.*

class StateMechanic(mlc: MythicLineConfig) : AbstractSkillMechanic(mlc), INoTargetSkill {

    private val model = mlc.modelPlaceholder
    private val state = mlc.toPlaceholderString(arrayOf("state", "s"))
    private val li = mlc.toPlaceholderInteger(arrayOf("li"), 1)
    private val lo = mlc.toPlaceholderInteger(arrayOf("lo"))
    private val sp = mlc.toNullablePlaceholderFloat(arrayOf("speed", "sp"))
    private val remove = mlc.toPlaceholderBoolean(arrayOf("remove", "r"))

    override fun cast(p0: SkillMetadata): SkillResult {
        val args = p0.toPlaceholderArgs()
        return p0.toTracker(model(args))?.let {
            val s = state(args) ?: return SkillResult.CONDITION_FAILED
            if (remove(args)) it.stopAnimation(s) else it.animate(s, AnimationModifier(
                li(args),
                lo(args),
                sp(args)?.let(FloatConstantSupplier::of)
            ))
            SkillResult.SUCCESS
        } ?: SkillResult.CONDITION_FAILED
    }
}