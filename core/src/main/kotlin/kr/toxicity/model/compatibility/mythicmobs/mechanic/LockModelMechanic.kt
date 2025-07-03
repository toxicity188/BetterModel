package kr.toxicity.model.compatibility.mythicmobs.mechanic

import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.INoTargetSkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import kr.toxicity.model.compatibility.mythicmobs.*

class LockModelMechanic(mlc: MythicLineConfig) : AbstractSkillMechanic(mlc), INoTargetSkill {

    private val model = mlc.modelPlaceholder
    private val lock = mlc.toPlaceholderBoolean(arrayOf("lock", "l"), true)

    override fun cast(p0: SkillMetadata): SkillResult {
        val args = p0.toPlaceholderArgs()
        return p0.toTracker(model(args))?.let {
            it.lockRotation(lock(args))
            SkillResult.SUCCESS
        } ?: SkillResult.CONDITION_FAILED
    }
}