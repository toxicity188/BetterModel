package kr.toxicity.model.compatibility.mythicmobs.condition

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.conditions.IEntityCondition
import kr.toxicity.model.compatibility.mythicmobs.*
import kr.toxicity.model.util.boneName

class ModelHasPassengerCondition(mlc: MythicLineConfig) : IEntityCondition {

    private val model = mlc.modelPlaceholder
    private val seat = mlc.toPlaceholderStringList(MM_SEAT) {
        it.map { s -> s.boneName }.toSet()
    }

    override fun check(p0: AbstractEntity): Boolean {
        val args = p0.toPlaceholderArgs()
        val set = seat(args)
        return p0.toTracker(model(args))?.let {
            if (set.isEmpty()) it.hasPassenger() else set.any { seat ->
                it.mountedHitBox().values.any { box ->
                    box.hitBox().positionSource().name == seat
                }
            }
        } == true
    }
}