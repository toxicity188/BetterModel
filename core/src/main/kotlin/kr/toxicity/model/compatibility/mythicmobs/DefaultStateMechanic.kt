package kr.toxicity.model.compatibility.mythicmobs

import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.INoTargetSkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.core.skills.SkillMechanic
import kr.toxicity.model.api.tracker.EntityTracker

class DefaultStateMechanic(mlc: MythicLineConfig) : SkillMechanic(MythicBukkit.inst().skillManager, null, "[BetterHud]", mlc), INoTargetSkill {

    private val t = mlc.getString(arrayOf("t", "type"))!!
    private val s = mlc.getString(arrayOf("state", "s"))!!

    init {
        isAsyncSafe = false
    }

    override fun cast(p0: SkillMetadata): SkillResult {
        return EntityTracker.tracker(p0.caster.entity.bukkitEntity)?.let {
            it.replaceLoop(t, s)
            SkillResult.SUCCESS
        } ?: SkillResult.ERROR
    }
}