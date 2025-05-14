package kr.toxicity.model.compatibility.mythicmobs.mechanic

import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.INoTargetSkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.core.skills.SkillMechanic
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.tracker.EntityTracker
import kr.toxicity.model.api.tracker.TrackerModifier
import kr.toxicity.model.api.util.EntityUtil
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderArgs
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderBoolean
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderFloat
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderString
import kr.toxicity.model.manager.ModelManagerImpl
import kr.toxicity.model.util.toPackName

class ModelMechanic(mlc: MythicLineConfig) : SkillMechanic(MythicBukkit.inst().skillManager, null, "", mlc), INoTargetSkill {

    private val mid = mlc.toPlaceholderString(arrayOf("mid", "m", "model")) {
        it?.toPackName()
    }
    private val s = mlc.toPlaceholderFloat(arrayOf("scale", "s"), 1F)
    private val st = mlc.toPlaceholderBoolean(arrayOf("sight-trace", "st"), true)
    private val de = mlc.toPlaceholderBoolean(arrayOf("damage-effect", "de"), true)
    private val vr = mlc.toPlaceholderFloat(arrayOf("view-range", "vr"), EntityUtil.ENTITY_MODEL_VIEW_RADIUS)
    private val shadow = mlc.toPlaceholderBoolean(arrayOf("shadow"), false)
    private val r = mlc.toPlaceholderBoolean(arrayOf("remove", "r"), false)

    init {
        isAsyncSafe = false
    }

    override fun cast(p0: SkillMetadata): SkillResult {
        val args = p0.toPlaceholderArgs()
        val e = p0.caster.entity.bukkitEntity
        return if (r(args)) {
            EntityTracker.tracker(e.uniqueId)?.run {
                close()
                SkillResult.SUCCESS
            } ?: SkillResult.CONDITION_FAILED
        } else {
            ModelManagerImpl.renderer(mid(args) ?: return SkillResult.CONDITION_FAILED)?.let {
                val created = it.create(e, TrackerModifier(
                    { s(args) },
                    st(args),
                    de(args),
                    vr(args),
                    shadow(args),
                    TrackerModifier.HideOption.DEFAULT)
                )
                BetterModel.inst().scheduler().taskLater(1, e) {
                    created.spawnNearby()
                }
                SkillResult.SUCCESS
            } ?: SkillResult.CONDITION_FAILED
        }
    }
}