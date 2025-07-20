package kr.toxicity.model.compatibility.mythicmobs.mechanic

import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.INoTargetSkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import kr.toxicity.model.api.tracker.TrackerUpdateAction
import kr.toxicity.model.compatibility.mythicmobs.*

class TintMechanic(mlc: MythicLineConfig) : AbstractSkillMechanic(mlc), INoTargetSkill {

    private val model = mlc.modelPlaceholder
    private val predicate = mlc.bonePredicateNullable
    private val damageTint = mlc.toPlaceholderBoolean(arrayOf("damagetint", "d", "dmg", "damage"))
    private val color = mlc.toPlaceholderColor(arrayOf("color", "c")) {
        it ?: 0xFFFFFF
    }

    override fun cast(p0: SkillMetadata): SkillResult {
        val args = p0.toPlaceholderArgs()
        return p0.toTracker(model(args))?.let {
            if (damageTint(args)) {
                it.damageTintValue(color(args))
            } else {
                it.cancelDamageTint()
                it.update(
                    TrackerUpdateAction.tint(color(args)),
                    predicate(args)
                )
            }
            SkillResult.SUCCESS
        } ?: SkillResult.CONDITION_FAILED
    }
}