package kr.toxicity.model.compatibility.mythicmobs.mechanic

import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.INoTargetSkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import io.lumine.mythic.bukkit.MythicBukkit
import kr.toxicity.model.api.nms.HitBoxListener
import kr.toxicity.model.compatibility.mythicmobs.*
import org.bukkit.entity.Damageable
import org.bukkit.entity.Entity

class BindHitBoxMechanic(mlc: MythicLineConfig) : AbstractSkillMechanic(mlc), INoTargetSkill {

    private val model = mlc.modelPlaceholder
    private val predicate = mlc.bonePredicate
    private val type = mlc.toPlaceholderString(arrayOf("type", "t", "mob", "m")) {
        if (it != null) MythicBukkit.inst().mobManager.getMythicMob(it).orElse(null) else null
    }

    init {
        isAsyncSafe = false
    }

    override fun cast(p0: SkillMetadata): SkillResult {
        val args = p0.toPlaceholderArgs()
        return p0.toTracker(model(args))?.let {
            val e = type(args) ?: return SkillResult.CONDITION_FAILED
            val spawned = e.spawn(p0.caster.location, p0.caster.level).apply {
                setParent(p0.caster)
                setOwnerUUID(p0.caster.entity.uniqueId)
            }.entity.bukkitEntity
            it.createHitBox(HitBoxListener.builder()
                .sync { hitBox ->
                    if (!spawned.isValid) hitBox.removeHitBox()
                    else spawned.teleportAsync((hitBox as Entity).location)
                }
                .damage { _, source, damage ->
                    if (spawned is Damageable) {
                        spawned.damage(damage, source.causingEntity)
                        true
                    } else false
                }
                .remove {
                    spawned.remove()
                }
                .build(), predicate(args))
            SkillResult.SUCCESS
        } ?: SkillResult.CONDITION_FAILED
    }
}