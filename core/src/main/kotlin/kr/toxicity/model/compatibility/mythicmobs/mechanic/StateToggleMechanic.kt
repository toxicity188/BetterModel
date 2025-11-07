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
import java.util.concurrent.ConcurrentHashMap

class StateToggleMechanic(mlc: MythicLineConfig) : AbstractSkillMechanic(mlc), INoTargetSkill {

    private val model = mlc.modelPlaceholder
    private val state = mlc.toPlaceholderString(arrayOf("state", "s"))
    private val li = mlc.toPlaceholderInteger(arrayOf("li"), 1)
    private val lo = mlc.toPlaceholderInteger(arrayOf("lo"))
    private val sp = mlc.toNullablePlaceholderFloat(arrayOf("speed", "sp"))
    private val delay = mlc.toPlaceholderInteger(arrayOf("delay", "d"), 0)

    private val toggleState = ConcurrentHashMap<String, Boolean>()

    override fun cast(meta: SkillMetadata): SkillResult {
        val args = meta.toPlaceholderArgs()
        return meta.toTracker(model(args))?.let { tracker ->
            val s = state(args) ?: return SkillResult.CONDITION_FAILED
            val modelId = model(args) ?: ""
            val key = "$modelId:$s"

            val isPlaying = toggleState.getOrPut(key) { false }

            if (isPlaying) {
                tracker.stopAnimation(s)
                toggleState[key] = false
            } else {
                tracker.animate(s, AnimationModifier(
                    li(args),
                    lo(args),
                    sp(args)?.let(FloatConstantSupplier::of)
                ))
                toggleState[key] = true
            }

            SkillResult.SUCCESS
        } ?: SkillResult.CONDITION_FAILED
    }
}
