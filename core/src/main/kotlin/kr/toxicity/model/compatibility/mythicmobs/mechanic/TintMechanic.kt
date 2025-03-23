package kr.toxicity.model.compatibility.mythicmobs.mechanic

import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.INoTargetSkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.core.skills.SkillMechanic
import kr.toxicity.model.api.tracker.EntityTracker

class TintMechanic(mlc: MythicLineConfig) : SkillMechanic(MythicBukkit.inst().skillManager, null, "", mlc), INoTargetSkill {

    private val part = mlc.getString(arrayOf("partid", "p", "pid", "part"))!!
    private val damageTint = mlc.getBoolean(arrayOf("damagetint", "d", "dmg", "damage"), false)
    private val color = mlc.getString(arrayOf("color", "c")).toInt(16)

    init {
        isAsyncSafe = false
    }

    override fun cast(p0: SkillMetadata): SkillResult {
        return EntityTracker.tracker(p0.caster.entity.bukkitEntity.uniqueId)?.let {
            if (damageTint) {
                it.damageTintValue(color)
            } else it.tint({ e -> e.name == part }, color)
            SkillResult.SUCCESS
        } ?: SkillResult.ERROR
    }
}