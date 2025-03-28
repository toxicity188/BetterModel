package kr.toxicity.model.compatibility.mythicmobs.mechanic

import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.INoTargetSkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.core.skills.SkillMechanic
import kr.toxicity.model.api.tracker.EntityTracker
import kr.toxicity.model.api.util.BonePredicate
import kr.toxicity.model.compatibility.mythicmobs.MM_CHILDREN
import kr.toxicity.model.compatibility.mythicmobs.MM_PART_ID

class BrightnessMechanic(mlc: MythicLineConfig) : SkillMechanic(MythicBukkit.inst().skillManager, null, "", mlc), INoTargetSkill {

    private val predicate = mlc.getString(MM_PART_ID).let { part ->
        if (part == null) BonePredicate.of(true) {
            true
        } else BonePredicate.of(mlc.getBoolean(MM_CHILDREN, false)) { b ->
            b.name == part
        }
    }
    private val block = mlc.getInteger("b", 0).coerceAtLeast(-1).coerceAtMost(15)
    private val sky = mlc.getInteger("s", 0).coerceAtLeast(-1).coerceAtMost(15)

    init {
        isAsyncSafe = false
    }

    override fun cast(p0: SkillMetadata): SkillResult {
        return EntityTracker.tracker(p0.caster.entity.bukkitEntity.uniqueId)?.let {
            if (it.brightness(predicate, block, sky)) it.forceUpdate(true)
            SkillResult.SUCCESS
        } ?: SkillResult.ERROR
    }
}