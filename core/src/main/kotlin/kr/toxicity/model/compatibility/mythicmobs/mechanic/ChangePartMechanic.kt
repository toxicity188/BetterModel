package kr.toxicity.model.compatibility.mythicmobs.mechanic

import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.INoTargetSkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.core.skills.SkillMechanic
import kr.toxicity.model.api.tracker.EntityTracker

class ChangePartMechanic(mlc: MythicLineConfig) : SkillMechanic(MythicBukkit.inst().skillManager, null, "[BetterModel]", mlc), INoTargetSkill {

    private val part = mlc.getString(arrayOf("part", "p"))!!
    private val newPart = mlc.getString(arrayOf("newpart", "np"))

    init {
        isAsyncSafe = false
    }

    override fun cast(p0: SkillMetadata): SkillResult {
        return EntityTracker.tracker(p0.caster.entity.bukkitEntity.uniqueId)?.let {
            it.entity(newPart)?.itemStack()?.let { item ->
                it.entity().forEach { e ->
                    e.itemStack({
                        it.name == part
                    }, item)
                }
            }
            SkillResult.SUCCESS
        } ?: SkillResult.ERROR
    }
}