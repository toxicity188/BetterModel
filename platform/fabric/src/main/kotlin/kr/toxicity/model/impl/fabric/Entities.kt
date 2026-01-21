/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.impl.fabric

import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.impl.fabric.entity.EntityHook
import kr.toxicity.model.impl.fabric.world.entityMap
import kr.toxicity.model.mixin.AvatarAccessor
import kr.toxicity.model.mixin.MobAccessor
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerPlayerConnection
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.ai.goal.RangedAttackGoal
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal
import net.minecraft.world.entity.ai.goal.RangedCrossbowAttackGoal
import net.minecraft.world.entity.animal.FlyingAnimal
import net.minecraft.world.entity.player.Player
import org.joml.Vector3f

fun Entity.toTracker(model: String?) = toRegistry()?.tracker(model)

fun Entity.toRegistry() = BetterModel.registryOrNull(uuid)

val Entity.isWalking: Boolean
    get() {
        return controllingPassenger?.isWalking ?: checkEntityWalkingState()
    }

val Entity.isFlying: Boolean
    get() {
        return this is FlyingAnimal && isFlying ||
            this is Mob && isNoAi ||
            this is Player && abilities.flying ||
            this is LivingEntity && isFallFlying
    }

val Entity.seenBy: Set<ServerPlayerConnection>
    get() {
        val level = level() as ServerLevel
        val tracker = level.chunkSource.chunkMap.entityMap.get(id)
            ?: return emptySet()

        return tracker.playersTracking
    }

var Entity.modelData: String?
    get() {
        return (this as EntityHook).`bettermodel$getModelData`()
    }
    set(value) {
        (this as EntityHook).`bettermodel$setModelData`(value)
    }

fun Entity.passengerPosition(dest: Vector3f): Vector3f {
    val point = attachments.get(EntityAttachment.PASSENGER, 0, yRot)
    return dest.set(
        point.x.toFloat(),
        point.y.toFloat(),
        point.z.toFloat()
    )
}

private fun Entity.checkEntityWalkingState(): Boolean {
    return when (this) {
        is Mob -> navigation.isInProgress || isRangedAttacking()
        is ServerPlayer -> xMovement() != 0.0f || zMovement() != 0.0f
        else -> false
    }
}

private fun Mob.isRangedAttacking(): Boolean {
    return (this as MobAccessor).`bettermodel$getGoalSelector`().availableGoals.any { wrapper ->
        wrapper.isRunning && wrapper.goal.isRangedAttackGoal()
    }
}

private fun Goal.isRangedAttackGoal(): Boolean {
    return this is RangedAttackGoal ||
        this is RangedCrossbowAttackGoal<*> ||
        this is RangedBowAttackGoal<*>
}

fun Avatar.getCustomisation(): Int {
    return entityData.get(
        AvatarAccessor.`bettermodel$getDataPlayerModeCustomisation`()
    ).toInt()
}

fun ServerPlayer.xMovement(): Float {
    return when {
        lastClientInput.left() == lastClientInput.right() -> 0.0f
        lastClientInput.left() -> 1.0f
        else -> -1.0f
    }
}

fun ServerPlayer.yMovement(): Float {
    return when {
        lastClientInput.jump -> 1.0f
        lastClientInput.shift -> -1.0f
        else -> 0.0f
    }
}

fun ServerPlayer.zMovement(): Float {
    return when {
        lastClientInput.forward() == lastClientInput.backward() -> 0.0f
        lastClientInput.forward() -> 1.0f
        else -> -1.0f
    }
}
