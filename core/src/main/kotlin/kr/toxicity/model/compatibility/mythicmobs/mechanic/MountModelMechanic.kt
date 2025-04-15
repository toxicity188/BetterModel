package kr.toxicity.model.compatibility.mythicmobs.mechanic

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.ITargetedEntitySkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.core.skills.SkillMechanic
import kr.toxicity.model.api.mount.MountControllers
import kr.toxicity.model.api.nms.HitBoxListener
import kr.toxicity.model.api.tracker.EntityTracker
import kr.toxicity.model.compatibility.mythicmobs.MM_SEAT
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderArgs
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderString
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderStringSet

class MountModelMechanic(mlc: MythicLineConfig) : SkillMechanic(MythicBukkit.inst().skillManager, null, "", mlc), ITargetedEntitySkill {

    companion object {
        private val dismountListener = HitBoxListener.builder()
            .dismount { h, _ ->
                h.removeHitBox()
            }
            .build()
    }

    private val interact = mlc.toPlaceholderString(arrayOf("mode", "m")) exec@ {
        when (it) {
            "walking" -> MountControllers.WALK.modifier()
            "force_walking" -> MountControllers.WALK.modifier()
                .canDismountBySelf(false)
            "flying" -> MountControllers.FLY.modifier()
            "force_flying" -> MountControllers.FLY.modifier()
                .canDismountBySelf(false)
            else -> return@exec MountControllers.WALK
        }.canControl(mlc.getBoolean(arrayOf("driver", "d", "drive"), true))
            .build()
    }

    private val seat = mlc.toPlaceholderStringSet(MM_SEAT)

    init {
        isAsyncSafe = false
    }

    override fun castAtEntity(p0: SkillMetadata, p1: AbstractEntity): SkillResult {
        val args = p0.toPlaceholderArgs()
        return EntityTracker.tracker(p0.caster.entity.bukkitEntity)?.let { tracker ->
            val set = seat(args)
            tracker.bone {
                set.contains(it.name)
                        && (it.hitBox?.hasMountDriver() == false)
                        && (it.hitBox != null || it.createHitBox(tracker.adapter, { true }, dismountListener))
            }?.let {
                it.hitBox?.let { hitBox ->
                    hitBox.mountController(interact(args))
                    hitBox.mount(p1.bukkitEntity)
                }
                SkillResult.SUCCESS
            }
        } ?: SkillResult.ERROR
    }
}