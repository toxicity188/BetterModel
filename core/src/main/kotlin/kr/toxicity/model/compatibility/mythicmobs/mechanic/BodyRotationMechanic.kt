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
import kr.toxicity.model.compatibility.mythicmobs.*

class BodyRotationMechanic(mlc: MythicLineConfig) : AbstractSkillMechanic(mlc), INoTargetSkill {

    private val model = mlc.modelPlaceholder
    private val headUneven = mlc.toNullablePlaceholderBoolean(arrayOf("headuneven", "hu", "head"))
    private val maxHead = mlc.toNullablePlaceholderFloat(arrayOf("maxhead", "mh", "mxh"))
    private val minHead = mlc.toNullablePlaceholderFloat(arrayOf("minhead", "mnh"))
    private val bodyUneven = mlc.toNullablePlaceholderBoolean(arrayOf("bodyuneven", "bu", "body"))
    private val maxBody = mlc.toNullablePlaceholderFloat(arrayOf("maxbody", "mb", "mxb"))
    private val minBody = mlc.toNullablePlaceholderFloat(arrayOf("minbody", "mnb"))
    private val playerMode = mlc.toNullablePlaceholderBoolean(arrayOf("playermode", "m", "mode", "player"))
    private val stable = mlc.toNullablePlaceholderFloat(arrayOf("stable", "s"))
    private val rDelay = mlc.toNullablePlaceholderInteger(arrayOf("rdelay", "rde"))
    private val rDuration = mlc.toNullablePlaceholderInteger(arrayOf("rduration", "rdu"))

    override fun cast(p0: SkillMetadata): SkillResult {
        val args = p0.toPlaceholderArgs()
        return p0.toTracker(model(args))?.bodyRotator()?.run {
            setValue { setter ->
                headUneven(args)?.let { setter.setHeadUneven(it) }
                maxHead(args)?.let { setter.setMaxHead(it) }
                minHead(args)?.let { setter.setMinHead(it) }
                bodyUneven(args)?.let { setter.setBodyUneven(it) }
                maxBody(args)?.let { setter.setMaxBody(it) }
                minBody(args)?.let { setter.setMinBody(it) }
                playerMode(args)?.let { setter.setPlayerMode(it) }
                stable(args)?.let { setter.setStable(it) }
                rDelay(args)?.let { setter.setRotationDelay(it) }
                rDuration(args)?.let { setter.setRotationDuration(it) }
            }
            SkillResult.SUCCESS
        } ?: SkillResult.CONDITION_FAILED
    }
}