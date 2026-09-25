/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.impl.fabric.entity

import kr.toxicity.model.api.mod.entity.BaseModEntity
import kr.toxicity.model.api.platform.PlatformEntity
import kr.toxicity.model.api.platform.PlatformPlayer
import kr.toxicity.model.api.util.TransformedItemStack
import kr.toxicity.model.impl.fabric.*
import kr.toxicity.model.impl.fabric.chat.asAdventure
import net.kyori.adventure.text.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import org.joml.Vector3f
import java.util.*
import java.util.stream.Stream

class BaseFabricEntityImpl(private var entity: Entity) : BaseModEntity {
    override fun entity(entity: Entity) {
        this.entity = entity
    }

    override fun platform(): PlatformEntity = entity.wrap()

    override fun customName(): Component? {
        return if (entity is ServerPlayer) {
            (entity.customName ?: entity.name).asAdventure()
        } else {
            entity.customName?.takeIf { entity.isCustomNameVisible }?.asAdventure()
        }
    }

    override fun handle(): Entity = entity

    override fun id(): Int = entity.id

    override fun dead(): Boolean {
        val entity = entity
        return entity.removalReason != null || entity is LivingEntity && entity.isDeadOrDying
    }

    override fun ground(): Boolean = entity.onGround()

    override fun invisible(): Boolean {
        val entity = entity
        return entity.isInvisible || entity is LivingEntity && entity.hasEffect(MobEffects.INVISIBILITY)
    }

    override fun glow(): Boolean = entity.isCurrentlyGlowing

    override fun onWalk(): Boolean = entity.isWalking

    override fun fly(): Boolean = entity.isFlying

    override fun scale(): Double {
        val entity = entity
        return if (entity is LivingEntity) {
            entity.scale.toDouble()
        } else {
            1.0
        }
    }

    override fun pitch(): Float = entity.xRot

    override fun bodyYaw(): Float = entity.let { if (it is LivingEntity) it.yBodyRot else it.yRot }

    override fun yaw(): Float = entity.yRot

    override fun headYaw(): Float = entity.let { if (it is LivingEntity) it.yHeadRot else it.yRot }

    override fun damageTick(): Float {
        val entity = entity
        if (entity !is LivingEntity || entity.invulnerableTime <= 0.0f) {
            return 0F
        }

        val knockbackResistant = entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE)?.value ?: 0.0
        val knockBack = 1 - knockbackResistant.toFloat()

        return entity.hurtTime.toFloat() / entity.hurtDuration * knockBack
    }

    override fun walkSpeed(): Float {
        val entity = entity
        if (entity !is LivingEntity) {
            return 0.0f
        }

        if (!entity.onGround()) {
            return 1.0f
        }

        val speed = entity.getEffect(MobEffects.SPEED)?.amplifier ?: 0
        val slow = entity.getEffect(MobEffects.SLOWNESS)?.amplifier ?: 0

        return (1.0f + (speed - slow) * 0.2f).coerceIn(0.2f..2.0f)
    }

    override fun passengerPosition(dest: Vector3f): Vector3f = entity.passengerPosition(dest)

    override fun trackedBy(): Stream<PlatformPlayer> = entity.seenBy.stream().map {
        it.wrap()
    }

    override fun mainHand(): TransformedItemStack {
        val entity = entity
        return if (entity is LivingEntity) {
            TransformedItemStack.of(entity.mainHandItem.wrap())
        } else {
            TransformedItemStack.empty()
        }
    }

    override fun offHand(): TransformedItemStack {
        val entity = entity
        return if (entity is LivingEntity) {
            TransformedItemStack.of(entity.offhandItem.wrap())
        } else {
            TransformedItemStack.empty()
        }
    }

    override fun modelData(): String? {
        return entity.modelData
    }

    override fun modelData(modelData: String?) {
        entity.modelData = modelData
    }

    override fun uuid(): UUID = entity.uuid
}
