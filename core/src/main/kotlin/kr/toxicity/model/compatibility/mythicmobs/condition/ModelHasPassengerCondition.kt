package kr.toxicity.model.compatibility.mythicmobs.condition

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.conditions.IEntityCondition
import kr.toxicity.model.compatibility.mythicmobs.*

class ModelHasPassengerCondition(mlc: MythicLineConfig) : IEntityCondition {

    private val model = mlc.modelPlaceholder
    private val seat = mlc.toPlaceholderStringList(MM_SEAT) {
        it.toSet()
    }

    override fun check(p0: AbstractEntity): Boolean {
        val args = p0.toPlaceholderArgs()
        val set = seat(args)
        return p0.toTracker(model(args))?.let {
            it.bones().any { b ->
                (set.isEmpty() || set.contains(b.name.name)) && b.hitBox?.hasMountDriver() == true
            }
        } == true
    }
}