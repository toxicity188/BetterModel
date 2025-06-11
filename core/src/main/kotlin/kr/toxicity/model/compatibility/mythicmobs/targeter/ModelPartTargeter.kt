package kr.toxicity.model.compatibility.mythicmobs.targeter

import io.lumine.mythic.api.adapters.AbstractLocation
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.targeters.ILocationTargeter
import kr.toxicity.model.compatibility.mythicmobs.MM_PART_ID
import kr.toxicity.model.compatibility.mythicmobs.modelPlaceholder
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderArgs
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderString
import kr.toxicity.model.compatibility.mythicmobs.toTracker
import kr.toxicity.model.util.boneName

class ModelPartTargeter(mlc: MythicLineConfig) : ILocationTargeter {

    private val model = mlc.modelPlaceholder
    private val part = mlc.toPlaceholderString(MM_PART_ID) {
        it?.boneName?.name
    }

    override fun getLocations(p0: SkillMetadata): Collection<AbstractLocation> {
        val args = p0.toPlaceholderArgs()
        return p0.toTracker(model(args))?.bone(part(args) ?: return emptyList())?.hitBoxPosition()?.let {
            listOf(p0.caster.entity.location.add(
                it.x.toDouble(),
                it.y.toDouble(),
                it.z.toDouble()
            ))
        } ?: emptyList()
    }
}