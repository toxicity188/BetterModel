package kr.toxicity.model.compatibility.mythicmobs.mechanic

import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.INoTargetSkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import kr.toxicity.model.api.tracker.EntityTrackerRegistry
import kr.toxicity.model.api.tracker.ModelScaler
import kr.toxicity.model.api.tracker.TrackerModifier
import kr.toxicity.model.api.util.EntityUtil
import kr.toxicity.model.compatibility.mythicmobs.modelPlaceholder
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderArgs
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderBoolean
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderFloat
import kr.toxicity.model.manager.ModelManagerImpl

class ModelMechanic(mlc: MythicLineConfig) : AbstractSkillMechanic(mlc), INoTargetSkill {

    private val mid = mlc.modelPlaceholder
    private val s = mlc.toPlaceholderFloat(arrayOf("scale", "s"), 1F)
    private val st = mlc.toPlaceholderBoolean(arrayOf("sight-trace", "st"), true)
    private val da = mlc.toPlaceholderBoolean(arrayOf("damageanimation", "da", "animation"), false)
    private val dt = mlc.toPlaceholderBoolean(arrayOf("damagetint", "tint", "dt"), true)
    private val vr = mlc.toPlaceholderFloat(arrayOf("view-range", "vr"), EntityUtil.ENTITY_MODEL_VIEW_RADIUS)
    private val r = mlc.toPlaceholderBoolean(arrayOf("remove", "r"), false)

    override fun cast(p0: SkillMetadata): SkillResult {
        val args = p0.toPlaceholderArgs()
        val e = p0.caster.entity.bukkitEntity
        return if (r(args)) {
            if (mid(args)?.let {
                EntityTrackerRegistry.registry(e.uniqueId)?.remove(it)
            } == true) SkillResult.SUCCESS else SkillResult.CONDITION_FAILED
        } else {
            ModelManagerImpl.renderer(mid(args) ?: return SkillResult.CONDITION_FAILED)?.let {
                it.create(e, TrackerModifier(
                    st(args),
                    da(args),
                    dt(args),
                    vr(args)
                )) { t ->
                    t.scaler(ModelScaler.entity().multiply(s(args)))
                }
                SkillResult.SUCCESS
            } ?: SkillResult.CONDITION_FAILED
        }
    }
}