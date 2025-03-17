package kr.toxicity.model.compatibility.mythicmobs.mechanic

import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.INoTargetSkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.core.skills.SkillMechanic
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.tracker.TrackerModifier
import kr.toxicity.model.manager.ModelManagerImpl

class ModelMechanic(mlc: MythicLineConfig) : SkillMechanic(MythicBukkit.inst().skillManager, null, "[BetterModel]", mlc), INoTargetSkill {

    private val mid = mlc.getString(arrayOf("mid", "m", "model"))!!
    private val s = mlc.getFloat(arrayOf("scale", "s"), 1F)
    private val st = mlc.getBoolean(arrayOf("sight-trace", "st"), true)
    private val de = mlc.getBoolean(arrayOf("damage-effect", "de"), true)

    init {
        isAsyncSafe = false
    }

    override fun cast(p0: SkillMetadata): SkillResult {
        return ModelManagerImpl.renderer(mid)?.let {
            val e = p0.caster.entity.bukkitEntity
            val created = it.create(e, TrackerModifier(s, st, de))
            BetterModel.inst().scheduler().taskLater(1, e.location) {
                created.spawnNearby(e.location)
            }
            SkillResult.SUCCESS
        } ?: SkillResult.ERROR
    }
}