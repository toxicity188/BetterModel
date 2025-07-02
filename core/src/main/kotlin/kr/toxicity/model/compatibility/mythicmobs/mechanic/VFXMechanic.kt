package kr.toxicity.model.compatibility.mythicmobs.mechanic

import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.INoTargetSkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import kr.toxicity.model.api.animation.AnimationModifier
import kr.toxicity.model.api.tracker.ModelScaler
import kr.toxicity.model.api.tracker.TrackerModifier
import kr.toxicity.model.compatibility.mythicmobs.*
import kr.toxicity.model.manager.ModelManagerImpl

class VFXMechanic(mlc: MythicLineConfig) : AbstractSkillMechanic(mlc), INoTargetSkill {

    private val mid = mlc.modelPlaceholder
    private val state = mlc.toPlaceholderString(arrayOf("state", "s"))
    private val st = mlc.toPlaceholderBoolean(arrayOf("sight-trace", "st"), true)
    private val scl = mlc.toPlaceholderFloat(arrayOf("scale"), 1F)
    private val spd = mlc.toPlaceholderFloat(arrayOf("speed"), 1F)

    override fun cast(p0: SkillMetadata): SkillResult {
        val args = p0.toPlaceholderArgs()
        val m1 = mid(args) ?: return SkillResult.CONDITION_FAILED
        val s1 = state(args) ?: return SkillResult.CONDITION_FAILED
        return ModelManagerImpl.renderer(m1)?.let {
            val e = p0.caster.entity.bukkitEntity
            val created = it.create(e, TrackerModifier(
                st(args),
                false,
                false,
                10F,
                false
            )) { t ->
                t.scaler(ModelScaler.value(scl(args)))
            }
            if (!created.animate(s1, AnimationModifier(0, 0, spd(args))) {
                created.close()
            }) created.close()
            SkillResult.SUCCESS
        } ?: SkillResult.CONDITION_FAILED
    }
}