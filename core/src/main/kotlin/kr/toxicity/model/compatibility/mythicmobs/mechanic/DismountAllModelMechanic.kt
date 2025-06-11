package kr.toxicity.model.compatibility.mythicmobs.mechanic

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.ITargetedEntitySkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import kr.toxicity.model.compatibility.mythicmobs.*

class DismountAllModelMechanic(mlc: MythicLineConfig) : AbstractSkillMechanic(mlc), ITargetedEntitySkill {

    private val model = mlc.modelPlaceholder
    private val seat = mlc.toPlaceholderStringList(MM_SEAT) {
        it.toSet()
    }

    init {
        isAsyncSafe = false
    }

    override fun castAtEntity(p0: SkillMetadata, p1: AbstractEntity): SkillResult {
        val args = toPlaceholderArgs(p0, p1)
        return p0.toTracker(model(args))?.let { tracker ->
            val set = seat(args)
            tracker.bones().forEach {
                if (set.isEmpty() || set.contains(it.name.name)) it.hitBox?.dismountAll()
            }
            SkillResult.SUCCESS
        } ?: SkillResult.CONDITION_FAILED
    }
}