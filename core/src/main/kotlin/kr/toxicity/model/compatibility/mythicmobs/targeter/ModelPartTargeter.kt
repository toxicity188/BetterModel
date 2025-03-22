package kr.toxicity.model.compatibility.mythicmobs.targeter

import io.lumine.mythic.api.adapters.AbstractLocation
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.targeters.ILocationTargeter
import kr.toxicity.model.api.tracker.EntityTracker

class ModelPartTargeter(mlc: MythicLineConfig) : ILocationTargeter {

    private val part = mlc.getString(arrayOf("p", "part"))

    override fun getLocations(p0: SkillMetadata): Collection<AbstractLocation> {
        val entity = p0.caster.entity
        val bukkit = entity.bukkitEntity
        val tracker = EntityTracker.tracker(bukkit.uniqueId) ?: return emptyList()
        return tracker.entity(part)?.worldPosition()?.let {
            listOf(entity.location.add(
                it.x.toDouble(),
                it.y.toDouble() + tracker.adapter.passengerPosition().y(),
                it.z.toDouble()
            ))
        } ?: emptyList()
    }
}