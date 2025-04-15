package kr.toxicity.model.compatibility.mythicmobs.condition

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.conditions.IEntityCondition
import kr.toxicity.model.api.tracker.EntityTracker
import kr.toxicity.model.compatibility.mythicmobs.MM_SEAT
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderArgs
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderStringSet

class ModelHasPassengerCondition(mlc: MythicLineConfig) : IEntityCondition {

    private val seat = mlc.toPlaceholderStringSet(MM_SEAT)

    override fun check(p0: AbstractEntity): Boolean {
        val set = seat(p0.toPlaceholderArgs())
        return EntityTracker.tracker(p0.bukkitEntity.uniqueId)?.let {
            set.isEmpty() || it.bones().any { b ->
                set.contains(b.name) && b.hitBox?.hasMountDriver() == true
            }
        } == true
    }
}