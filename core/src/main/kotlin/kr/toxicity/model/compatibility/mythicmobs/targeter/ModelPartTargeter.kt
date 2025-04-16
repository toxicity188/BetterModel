package kr.toxicity.model.compatibility.mythicmobs.targeter

import io.lumine.mythic.api.adapters.AbstractLocation
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.targeters.ILocationTargeter
import kr.toxicity.model.api.tracker.EntityTracker
import kr.toxicity.model.compatibility.mythicmobs.MM_PART_ID
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderArgs
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderString

class ModelPartTargeter(mlc: MythicLineConfig) : ILocationTargeter {

    private val part = mlc.toPlaceholderString(MM_PART_ID)

    override fun getLocations(p0: SkillMetadata): Collection<AbstractLocation> {
        val args = p0.toPlaceholderArgs()
        val entity = p0.caster.entity
        val bukkit = entity.bukkitEntity
        val tracker = EntityTracker.tracker(bukkit.uniqueId) ?: return emptyList()
        return tracker.bone(part(args) ?: return emptyList())?.worldPosition()?.let {
            listOf(entity.location.add(
                it.x.toDouble(),
                it.y.toDouble(),
                it.z.toDouble()
            ))
        } ?: emptyList()
    }
}