package kr.toxicity.model.compatibility.mythicmobs.mechanic

import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.INoTargetSkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.core.skills.SkillMechanic
import kr.toxicity.model.api.event.ModelDamageSource
import kr.toxicity.model.api.nms.HitBox
import kr.toxicity.model.api.nms.HitBoxListener
import kr.toxicity.model.api.tracker.EntityTracker
import kr.toxicity.model.compatibility.mythicmobs.MM_PART_ID
import org.bukkit.entity.Damageable

class BindHitBoxMechanic(mlc: MythicLineConfig) : SkillMechanic(MythicBukkit.inst().skillManager, null, "", mlc), INoTargetSkill {

    private val partId = mlc.getString(MM_PART_ID)!!
    private val type = mlc.getString(arrayOf("type", "t", "mob", "m"))?.let {
        MythicBukkit.inst().mobManager.getMythicMob(it).orElseThrow()
    }

    init {
        isAsyncSafe = false
    }

    override fun cast(p0: SkillMetadata): SkillResult {
        return EntityTracker.tracker(p0.caster.entity.bukkitEntity)?.let {
            val handler = type?.let { e ->
                val spawned = e.spawn(p0.caster.location, p0.caster.level).entity.bukkitEntity
                object : HitBoxListener {
                    override fun sync(hitBox: HitBox) {
                        if (!spawned.isValid) hitBox.removeHitBox()
                        else spawned.teleport(it.entity.location)
                    }

                    override fun damage(source: ModelDamageSource, damage: Double): Boolean {
                        if (spawned is Damageable) {
                            spawned.damage(damage, source.causingEntity)
                            return true
                        } else return false
                    }

                    override fun remove(hitBox: HitBox) {
                        spawned.remove()
                    }

                }
            } ?: HitBoxListener.EMPTY
            it.createHitBox({ e ->
                e.name == partId
            }, handler)
            SkillResult.SUCCESS
        } ?: SkillResult.ERROR
    }
}