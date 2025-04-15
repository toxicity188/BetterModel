package kr.toxicity.model.compatibility.mythicmobs.mechanic

import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.INoTargetSkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.core.skills.SkillMechanic
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.tracker.TrackerModifier
import kr.toxicity.model.api.util.EntityUtil
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderArgs
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderBoolean
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderFloat
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderString
import kr.toxicity.model.manager.ModelManagerImpl

class ModelMechanic(mlc: MythicLineConfig) : SkillMechanic(MythicBukkit.inst().skillManager, null, "", mlc), INoTargetSkill {

    private val mid = mlc.toPlaceholderString(arrayOf("mid", "m", "model"))
    private val s = mlc.toPlaceholderFloat(arrayOf("scale", "s"), 1F)
    private val st = mlc.toPlaceholderBoolean(arrayOf("sight-trace", "st"), true)
    private val de = mlc.toPlaceholderBoolean(arrayOf("damage-effect", "de"), true)
    private val vr = mlc.toPlaceholderFloat(arrayOf("view-range", "vr"), EntityUtil.ENTITY_MODEL_VIEW_RADIUS)
    private val shadow = mlc.toPlaceholderBoolean(arrayOf("shadow"), false)

    init {
        isAsyncSafe = false
    }

    override fun cast(p0: SkillMetadata): SkillResult {
        val args = p0.toPlaceholderArgs()
        return ModelManagerImpl.renderer(mid(args) ?: return SkillResult.ERROR)?.let {
            val e = p0.caster.entity.bukkitEntity
            val created = it.create(e, TrackerModifier(s(args), st(args), de(args), vr(args), shadow(args)))
            BetterModel.inst().scheduler().taskLater(1, e.location) {
                created.spawnNearby()
            }
            SkillResult.SUCCESS
        } ?: SkillResult.ERROR
    }
}