package kr.toxicity.model.compatibility.mythicmobs.mechanic

import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.INoTargetSkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.core.skills.SkillMechanic
import kr.toxicity.model.api.animation.AnimationModifier
import kr.toxicity.model.api.tracker.TrackerModifier
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderArgs
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderBoolean
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderFloat
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderString
import kr.toxicity.model.manager.ModelManagerImpl

class VFXMechanic(mlc: MythicLineConfig) : SkillMechanic(MythicBukkit.inst().skillManager, null, "", mlc), INoTargetSkill {

    private val mid = mlc.toPlaceholderString(arrayOf("mid", "m", "model"))
    private val state = mlc.toPlaceholderString(arrayOf("state", "s"))
    private val st = mlc.toPlaceholderBoolean(arrayOf("sight-trace", "st"), true)
    private val scl = mlc.toPlaceholderFloat(arrayOf("scale"), 1F)
    private val spd = mlc.toPlaceholderFloat(arrayOf("speed"), 1F)

    init {
        isAsyncSafe = false
    }

    override fun cast(p0: SkillMetadata): SkillResult {
        val args = p0.toPlaceholderArgs()
        val m1 = mid(args) ?: return SkillResult.ERROR
        val s1 = state(args) ?: return SkillResult.ERROR
        return ModelManagerImpl.renderer(m1)?.let {
            val e = p0.caster.entity.bukkitEntity
            val created = it.create(e, TrackerModifier({ scl(args) }, st(args), false, 0F, false))
            if (created.animateSingle(s1, AnimationModifier(0, 0, spd(args))) {
                created.close()
            }) created.spawnNearby() else created.close()
            SkillResult.SUCCESS
        } ?: SkillResult.ERROR
    }
}