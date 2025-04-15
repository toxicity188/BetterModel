package kr.toxicity.model.compatibility.mythicmobs.mechanic

import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.INoTargetSkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.core.skills.SkillMechanic
import kr.toxicity.model.api.tracker.EntityTracker
import kr.toxicity.model.compatibility.mythicmobs.bonePredicateNullable
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderArgs
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderInteger

class BrightnessMechanic(mlc: MythicLineConfig) : SkillMechanic(MythicBukkit.inst().skillManager, null, "", mlc), INoTargetSkill {

    private val predicate = mlc.bonePredicateNullable
    private val block = mlc.toPlaceholderInteger(arrayOf("block", "b")) {
        it.coerceAtLeast(-1).coerceAtMost(15)
    }
    private val sky = mlc.toPlaceholderInteger(arrayOf("sky", "s")) {
        it.coerceAtLeast(-1).coerceAtMost(15)
    }

    override fun cast(p0: SkillMetadata): SkillResult {
        val args = p0.toPlaceholderArgs()
        return EntityTracker.tracker(p0.caster.entity.bukkitEntity.uniqueId)?.let {
            if (it.brightness(predicate(args), block(args), sky(args))) it.forceUpdate(true)
            SkillResult.SUCCESS
        } ?: SkillResult.ERROR
    }
}