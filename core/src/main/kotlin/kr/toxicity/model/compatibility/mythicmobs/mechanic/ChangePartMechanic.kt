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
import kr.toxicity.model.script.ChangePartScript
import kr.toxicity.model.util.boneName

class ChangePartMechanic(mlc: MythicLineConfig) : AbstractSkillMechanic(mlc), INoTargetSkill {

    private val model = mlc.modelPlaceholder
    private val predicate = mlc.bonePredicate
    private val nmid = mlc.toPlaceholderString(arrayOf("newmodelid", "nm", "nmid", "newmodel"))
    private val newPart = mlc.toPlaceholderString(arrayOf("newpart", "np", "npid")) {
        it?.boneName
    }

    override fun cast(p0: SkillMetadata): SkillResult {
        val args = p0.toPlaceholderArgs()
        return p0.toTracker(model(args))?.let {
            ChangePartScript(
                predicate(args),
                nmid(args) ?: return SkillResult.CONDITION_FAILED,
                newPart(args) ?: return SkillResult.CONDITION_FAILED
            ).accept(it)
            SkillResult.SUCCESS
        } ?: SkillResult.CONDITION_FAILED
    }
}