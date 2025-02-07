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
        return EntityTracker.tracker(bukkit.uniqueId)?.entity(part)?.lastTransform()?.let {
            listOf(entity.location.add(
                it.x.toDouble(),
                it.y.toDouble(),
                it.z.toDouble()
            ))
        } ?: emptyList()
    }
}