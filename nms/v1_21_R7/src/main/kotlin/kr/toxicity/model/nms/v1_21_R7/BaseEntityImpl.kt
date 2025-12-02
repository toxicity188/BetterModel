/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.nms.v1_21_R7

import kr.toxicity.model.api.entity.BaseBukkitEntity
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import org.bukkit.Location
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataHolder
import org.joml.Vector3f
import java.util.*
import java.util.stream.Stream

internal data class BaseEntityImpl(
    private val delegate: CraftEntity
) : BaseBukkitEntity, PersistentDataHolder by delegate {
    override fun customName(): AdventureComponent? = handle().run {
        if (this is ServerPlayer) (customName ?: name).asAdventure() else customName?.asAdventure()?.takeIf {
            isCustomNameVisible
        }
    }

    override fun entity(): org.bukkit.entity.Entity = delegate
    override fun handle(): Entity = delegate.vanillaEntity
    override fun uuid(): UUID = delegate.uniqueId
    override fun id(): Int = handle().id
    override fun dead(): Boolean = (handle() as? LivingEntity)?.isDeadOrDying == true || handle().removalReason != null || !handle().valid
    override fun invisible(): Boolean = handle().isInvisible || (handle() as? LivingEntity)?.hasEffect(MobEffects.INVISIBILITY) == true
    override fun glow(): Boolean = handle().isCurrentlyGlowing

    override fun onWalk(): Boolean {
        return handle().isWalking()
    }

    override fun scale(): Double {
        val handle = handle()
        return if (handle is LivingEntity) handle.scale.toDouble() else 1.0
    }

    override fun pitch(): Float = handle().xRot
    override fun ground(): Boolean = handle().onGround()
    override fun bodyYaw(): Float = handle().yRot
    override fun headYaw(): Float = if (handle() is LivingEntity) handle().yHeadRot else bodyYaw()
    override fun fly(): Boolean = handle().isFlying

    override fun damageTick(): Float {
        val handle = handle()
        if (handle !is LivingEntity) return 0F
        val duration = handle.invulnerableDuration.toFloat()
        if (duration <= 0F) return 0F
        val knockBack = 1 - (handle.getAttribute(Attributes.KNOCKBACK_RESISTANCE)?.value?.toFloat() ?: 0F)
        return handle.invulnerableTime.toFloat() / duration * knockBack
    }

    override fun walkSpeed(): Float {
        val handle = handle()
        if (handle !is LivingEntity) return 0F
        if (!handle.onGround) return 1F
        val speed = handle.getEffect(MobEffects.SPEED)?.amplifier ?: 0
        val slow = handle.getEffect(MobEffects.SLOWNESS)?.amplifier ?: 0
        return (1F + (speed - slow) * 0.2F)
            .coerceAtLeast(0.2F)
            .coerceAtMost(2F)
    }

    override fun passengerPosition(): Vector3f {
        return handle().passengerPosition()
    }

    override fun trackedBy(): Stream<Player> = delegate.trackedBy.stream()
    override fun location(): Location = delegate.location
}
