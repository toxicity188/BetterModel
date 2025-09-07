package kr.toxicity.model.compatibility.mythicmobs.mechanic

import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.INoTargetSkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import kr.toxicity.model.compatibility.mythicmobs.modelPlaceholder
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderArgs
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderString
import kr.toxicity.model.compatibility.mythicmobs.toTracker
import kr.toxicity.model.script.RemapScript

class RemapModelMechanic(mlc: MythicLineConfig) : AbstractSkillMechanic(mlc), INoTargetSkill {

    private val modelId = mlc.modelPlaceholder
    private val newModelId = mlc.toPlaceholderString(arrayOf("newmodelid", "n", "nid", "newmodel"))
    private val map = mlc.toPlaceholderString(arrayOf("map"))

    override fun cast(p0: SkillMetadata): SkillResult {
        val args = p0.toPlaceholderArgs()
        val m = modelId(args) ?: return SkillResult.INVALID_CONFIG
        return p0.toTracker(m)?.let {
            RemapScript(
                newModelId(args) ?: return SkillResult.INVALID_CONFIG,
                map(args)
            ).accept(it)
            SkillResult.SUCCESS
        } ?: SkillResult.CONDITION_FAILED
    }
}