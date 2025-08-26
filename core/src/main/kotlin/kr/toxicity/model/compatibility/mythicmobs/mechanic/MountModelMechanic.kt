package kr.toxicity.model.compatibility.mythicmobs.mechanic

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.ITargetedEntitySkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import kr.toxicity.model.api.mount.MountControllers
import kr.toxicity.model.api.nms.HitBoxListener
import kr.toxicity.model.compatibility.mythicmobs.*

class MountModelMechanic(mlc: MythicLineConfig) : AbstractSkillMechanic(mlc), ITargetedEntitySkill {

    companion object {
        private val dismountListener = HitBoxListener.builder()
            .dismount { h, _ ->
                h.removeHitBox()
            }
            .build()
    }

    private val model = mlc.modelPlaceholder
    private val driver = mlc.toPlaceholderBoolean(arrayOf("driver", "d", "drive"), true)
    private val damagemount = mlc.toPlaceholderBoolean(arrayOf("damagemount", "dmg"), false)
    private val interact = mlc.toPlaceholderString(arrayOf("mode", "m")) exec@ {
        when (it) {
            "walking" -> MountControllers.WALK.modifier()
            "force_walking" -> MountControllers.WALK.modifier()
                .canDismountBySelf(false)
            "flying" -> MountControllers.FLY.modifier()
            "force_flying" -> MountControllers.FLY.modifier()
                .canDismountBySelf(false)
            else -> null
        }?.canMount(false)
    }

    private val seat = mlc.toPlaceholderStringList(MM_SEAT) {
        it.toSet()
    }

    init {
        isAsyncSafe = false
    }

    override fun castAtEntity(p0: SkillMetadata, p1: AbstractEntity): SkillResult {
        val args = toPlaceholderArgs(p0, p1)
        return p0.toTracker(model(args))?.let { tracker ->
            val set = seat(args)
            tracker.bone {
                (set.isEmpty() || set.contains(it.name().name))
                        && it.hitBox?.hasMountDriver() != true
                        && (it.hitBox != null || it.createHitBox(tracker.registry().adapter(), { true }, dismountListener))
            }?.let {
                it.hitBox?.let { hitBox ->
                    hitBox.mountController(interact(args)
                        ?.canControl(driver(args))
                        ?.canBeDamagedByRider(damagemount(args))
                        ?.build()
                        ?: MountControllers.WALK)
                    hitBox.mount(p1.bukkitEntity)
                }
                SkillResult.SUCCESS
            }
        } ?: SkillResult.CONDITION_FAILED
    }
}