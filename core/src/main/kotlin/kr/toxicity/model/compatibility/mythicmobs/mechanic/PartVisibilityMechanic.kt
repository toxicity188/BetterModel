package kr.toxicity.model.compatibility.mythicmobs.mechanic

import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.INoTargetSkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import kr.toxicity.model.api.tracker.TrackerUpdateAction
import kr.toxicity.model.compatibility.mythicmobs.*

class PartVisibilityMechanic(mlc: MythicLineConfig) : AbstractSkillMechanic(mlc), INoTargetSkill {

    private val model = mlc.modelPlaceholder
    private val predicate = mlc.bonePredicateNullable
    private val v = mlc.toPlaceholderBoolean(arrayOf("visibility", "visible", "v"), true)

    override fun cast(p0: SkillMetadata): SkillResult {
        val args = p0.toPlaceholderArgs()
        return p0.toTracker(model(args))?.let {
            it.update(
                TrackerUpdateAction.TOGGLE_PART,
                TrackerUpdateAction.togglePart(v(args)),
                predicate(args)
            )
            SkillResult.SUCCESS
        } ?: SkillResult.CONDITION_FAILED
    }
}