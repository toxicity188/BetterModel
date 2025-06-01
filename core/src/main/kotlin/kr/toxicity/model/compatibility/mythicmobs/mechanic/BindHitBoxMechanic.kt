package kr.toxicity.model.compatibility.mythicmobs.mechanic

import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.INoTargetSkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.core.skills.SkillMechanic
import kr.toxicity.model.api.nms.HitBoxListener
import kr.toxicity.model.compatibility.mythicmobs.bonePredicate
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderArgs
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderString
import kr.toxicity.model.compatibility.mythicmobs.toTracker
import org.bukkit.entity.Damageable
import org.bukkit.entity.Entity

class BindHitBoxMechanic(mlc: MythicLineConfig) : SkillMechanic(MythicBukkit.inst().skillManager, null, "", mlc), INoTargetSkill {

    private val predicate = mlc.bonePredicate
    private val type = mlc.toPlaceholderString(arrayOf("type", "t", "mob", "m")) {
        if (it != null) MythicBukkit.inst().mobManager.getMythicMob(it).orElse(null) else null
    }

    init {
        isAsyncSafe = false
    }

    override fun cast(p0: SkillMetadata): SkillResult {
        val args = p0.toPlaceholderArgs()
        return p0.toTracker()?.let {
            val e = type(args) ?: return SkillResult.CONDITION_FAILED
            val spawned = e.spawn(p0.caster.location, p0.caster.level).apply {
                setParent(p0.caster)
                setOwnerUUID(p0.caster.entity.uniqueId)
            }.entity.bukkitEntity
            it.createHitBox(predicate(args), HitBoxListener.builder()
                .sync { hitBox ->
                    if (!spawned.isValid) hitBox.removeHitBox()
                    else spawned.teleportAsync((hitBox as Entity).location)
                }
                .damage { source, damage ->
                    if (spawned is Damageable) {
                        spawned.damage(damage, source.causingEntity)
                        true
                    } else false
                }
                .remove { hitBox ->
                    spawned.remove()
                }
                .build())
            SkillResult.SUCCESS
        } ?: SkillResult.CONDITION_FAILED
    }
}