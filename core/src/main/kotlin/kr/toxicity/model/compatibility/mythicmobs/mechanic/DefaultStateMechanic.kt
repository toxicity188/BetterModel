package kr.toxicity.model.compatibility.mythicmobs.mechanic

import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.INoTargetSkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.core.skills.SkillMechanic
import kr.toxicity.model.api.animation.AnimationModifier
import kr.toxicity.model.api.tracker.EntityTracker
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderArgs
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderFloat
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderInteger
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderString

class DefaultStateMechanic(mlc: MythicLineConfig) : SkillMechanic(MythicBukkit.inst().skillManager, null, "", mlc), INoTargetSkill {

    private val type = mlc.toPlaceholderString(arrayOf("t", "type"))
    private val state = mlc.toPlaceholderString(arrayOf("state", "s"))
    private val li = mlc.toPlaceholderInteger(arrayOf("li"))
    private val lo = mlc.toPlaceholderInteger(arrayOf("lo"))
    private val sp = mlc.toPlaceholderFloat(arrayOf("speed", "sp"), 1F)

    override fun cast(p0: SkillMetadata): SkillResult {
        val args = p0.toPlaceholderArgs()
        return EntityTracker.tracker(p0.caster.entity.bukkitEntity)?.let {
            val t = type(args)
            val s = state(args)
            if (t == null || s == null) return SkillResult.CONDITION_FAILED
            it.replace(t, s, AnimationModifier(li(args), lo(args), sp(args)))
            SkillResult.SUCCESS
        } ?: SkillResult.CONDITION_FAILED
    }
}