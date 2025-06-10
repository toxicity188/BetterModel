package kr.toxicity.model.compatibility.mythicmobs.mechanic

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.ITargetedEntitySkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.core.skills.SkillMechanic
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.animation.AnimationIterator
import kr.toxicity.model.api.animation.AnimationModifier
import kr.toxicity.model.api.tracker.EntityTracker
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderBoolean
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderFloat
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderString
import kr.toxicity.model.compatibility.mythicmobs.toPlaceholderArgs
import org.bukkit.entity.Player
import java.util.function.BooleanSupplier

class PlayLimbAnimMechanic(mlc: MythicLineConfig) : SkillMechanic(MythicBukkit.inst().skillManager, null, "", mlc), ITargetedEntitySkill {

    private val modelId = mlc.toPlaceholderString(arrayOf("model", "m"))
    private val animationId = mlc.toPlaceholderString(arrayOf("animation", "anim", "a"))
    private val speed = mlc.toPlaceholderFloat(arrayOf("speed", "sp"), 1.0f)
    private val remove = mlc.toPlaceholderBoolean(arrayOf("remove", "r"), false)
    private val mode = mlc.toPlaceholderString(arrayOf("mode", "loop"), "once")

    override fun castAtEntity(data: SkillMetadata, target: AbstractEntity): SkillResult {
        val targetPlayer = target.bukkitEntity as? Player ?: return SkillResult.CONDITION_FAILED
        val args = toPlaceholderArgs(data, target)

        val currentModelId = modelId(args) ?: return SkillResult.INVALID_CONFIG
        val currentAnimationId = animationId(args) ?: if (!remove(args)) return SkillResult.INVALID_CONFIG else ""

        if (remove(args)) {
            val tracker = EntityTracker.tracker(targetPlayer.uniqueId)
            if (tracker != null && tracker.name() == currentModelId) {
                tracker.close()
            }
        } else {
            val renderer = BetterModel.plugin().playerManager().limb(currentModelId)
                ?: return SkillResult.CONDITION_FAILED.also {
                    println("Error: Player not found'$currentModelId'")
                }

            EntityTracker.tracker(targetPlayer.uniqueId)?.close()
            val newTracker = renderer.create(targetPlayer)
            newTracker.spawnNearby()

            val loopType = when (mode(args)?.lowercase()) {
                "loop" -> AnimationIterator.Type.LOOP
                "hold" -> AnimationIterator.Type.HOLD_ON_LAST
                else -> AnimationIterator.Type.PLAY_ONCE
            }

            val predicate = BooleanSupplier { true }
            val speedModifier = AnimationModifier.SpeedModifier(speed(args))
            val modifier = AnimationModifier(predicate, 0, 0, loopType, speedModifier)

            val onEndTask: Runnable = if (loopType == AnimationIterator.Type.PLAY_ONCE) {
                Runnable { newTracker.close() }
            } else {
                Runnable {}
            }

            if (!newTracker.animate(currentAnimationId, modifier, onEndTask)) {
                newTracker.close()
            }
        }
        return SkillResult.SUCCESS
    }
}