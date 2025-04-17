package kr.toxicity.model.compatibility.mythicmobs.condition

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.conditions.IEntityCondition
import kr.toxicity.model.api.tracker.EntityTracker
import kr.toxicity.model.compatibility.mythicmobs.MM_SEAT
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderArgs
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderStringList

class ModelHasPassengerCondition(mlc: MythicLineConfig) : IEntityCondition {

    private val seat = mlc.toPlaceholderStringList(MM_SEAT) {
        it.toSet()
    }

    override fun check(p0: AbstractEntity): Boolean {
        val set = seat(p0.toPlaceholderArgs())
        return EntityTracker.tracker(p0.bukkitEntity.uniqueId)?.let {
            it.bones().any { b ->
                (set.isEmpty() || set.contains(b.name.name)) && b.hitBox?.hasMountDriver() == true
            }
        } == true
    }
}