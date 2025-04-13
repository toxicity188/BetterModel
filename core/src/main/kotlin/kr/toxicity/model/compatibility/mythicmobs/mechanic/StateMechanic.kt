package kr.toxicity.model.compatibility.mythicmobs.mechanic

import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.INoTargetSkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.core.skills.SkillMechanic
import kr.toxicity.model.api.animation.AnimationModifier
import kr.toxicity.model.api.tracker.EntityTracker

class StateMechanic(mlc: MythicLineConfig) : SkillMechanic(MythicBukkit.inst().skillManager, null, "", mlc), INoTargetSkill {

    private val s = mlc.getString(arrayOf("state", "s"))!!
    private val li = mlc.getInteger(arrayOf("li"), 0)
    private val lo = mlc.getInteger(arrayOf("lo"), 0)
    private val sp = mlc.getFloat(arrayOf("speed", "sp"), 1F)
    private val remove = mlc.getBoolean(arrayOf("remove", "r"), false)

    override fun cast(p0: SkillMetadata): SkillResult {
        return EntityTracker.tracker(p0.caster.entity.bukkitEntity)?.let {
            if (remove) it.stopAnimation(s) else it.animateSingle(s, AnimationModifier(li, lo, sp))
            SkillResult.SUCCESS
        } ?: SkillResult.ERROR
    }
}