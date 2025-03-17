package kr.toxicity.model.compatibility.mythicmobs.mechanic

import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.INoTargetSkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.core.skills.SkillMechanic
import kr.toxicity.model.api.data.renderer.AnimationModifier
import kr.toxicity.model.api.tracker.TrackerModifier
import kr.toxicity.model.manager.ModelManagerImpl

class VFXMechanic(mlc: MythicLineConfig) : SkillMechanic(MythicBukkit.inst().skillManager, null, "[BetterModel]", mlc), INoTargetSkill {

    private val mid = mlc.getString(arrayOf("mid", "m", "model"))!!
    private val state = mlc.getString(arrayOf("state", "s"))!!
    private val st = mlc.getBoolean(arrayOf("sight-trace", "st"), true)
    private val scl = mlc.getFloat(arrayOf("scale"), 1F)
    private val spd = mlc.getFloat(arrayOf("speed"), 1F)

    init {
        isAsyncSafe = false
    }

    override fun cast(p0: SkillMetadata): SkillResult {
        return ModelManagerImpl.renderer(mid)?.let {
            val e = p0.caster.entity.bukkitEntity
            val created = it.create(e, TrackerModifier(scl, st, false))
            if (created.animateSingle(state, AnimationModifier({ true }, 0, 0, spd)) {
                created.close()
            }) created.spawnNearby(e.location) else created.close()
            SkillResult.SUCCESS
        } ?: SkillResult.ERROR
    }
}