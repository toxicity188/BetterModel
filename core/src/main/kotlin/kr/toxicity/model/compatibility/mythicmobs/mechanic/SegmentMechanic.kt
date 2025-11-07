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
import kr.toxicity.model.script.SegmentScript

class SegmentMechanic(mlc: MythicLineConfig) : AbstractSkillMechanic(mlc), INoTargetSkill {

    private val model = mlc.modelPlaceholder
    private val bonePredicate = mlc.bonePredicate

    private val bounded = mlc.toNullablePlaceholderBoolean(arrayOf("bounded", "b"))
    private val minX = mlc.toNullablePlaceholderFloat(arrayOf("minx", "mix"))
    private val maxX = mlc.toNullablePlaceholderFloat(arrayOf("maxx", "max"))
    private val minY = mlc.toNullablePlaceholderFloat(arrayOf("miny", "miy"))
    private val maxY = mlc.toNullablePlaceholderFloat(arrayOf("maxy", "may"))
    private val minZ = mlc.toNullablePlaceholderFloat(arrayOf("minz", "miz"))
    private val maxZ = mlc.toNullablePlaceholderFloat(arrayOf("maxz", "maz"))
    private val alignRate = mlc.toNullablePlaceholderFloat(arrayOf("alignrate", "align", "ar"))
    private val elasticity = mlc.toNullablePlaceholderFloat(arrayOf("elasticity", "elastic", "e"))

    override fun cast(meta: SkillMetadata): SkillResult {
        val args = meta.toPlaceholderArgs()
        return meta.toTracker(model(args))?.let { tracker ->
            SegmentScript(
                bonePredicate(args),
                bounded(args),
                minX(args),
                maxX(args),
                minY(args),
                maxY(args),
                minZ(args),
                maxZ(args),
                alignRate(args),
                elasticity(args)
            ).accept(tracker)
            SkillResult.SUCCESS
        } ?: SkillResult.CONDITION_FAILED
    }
}
