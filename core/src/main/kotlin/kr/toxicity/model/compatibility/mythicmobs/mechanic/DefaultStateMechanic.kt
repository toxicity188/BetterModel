package kr.toxicity.model.compatibility.mythicmobs.mechanic

import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.INoTargetSkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import kr.toxicity.model.api.animation.AnimationModifier
import kr.toxicity.model.compatibility.mythicmobs.*

class DefaultStateMechanic(mlc: MythicLineConfig) : AbstractSkillMechanic(mlc), INoTargetSkill {

    private val model = mlc.modelPlaceholder
    private val type = mlc.toPlaceholderString(arrayOf("t", "type"))
    private val state = mlc.toPlaceholderString(arrayOf("state", "s"))
    private val li = mlc.toPlaceholderInteger(arrayOf("li"), 1)
    private val lo = mlc.toPlaceholderInteger(arrayOf("lo"))
    private val sp = mlc.toPlaceholderFloat(arrayOf("speed", "sp"), 1F)

    override fun cast(p0: SkillMetadata): SkillResult {
        val args = p0.toPlaceholderArgs()
        return p0.toTracker(model(args))?.let {
            val t = type(args)
            val s = state(args)
            if (t == null || s == null) return SkillResult.CONDITION_FAILED
            it.replace(t, s, AnimationModifier(li(args), lo(args), sp(args)))
            SkillResult.SUCCESS
        } ?: SkillResult.CONDITION_FAILED
    }
}