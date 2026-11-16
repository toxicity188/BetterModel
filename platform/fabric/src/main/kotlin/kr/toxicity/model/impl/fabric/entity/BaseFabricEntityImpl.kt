/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.impl.fabric.entity

import kr.toxicity.model.api.animation.keyframe.KeyframeRuntimeContext
import kr.toxicity.model.api.mod.entity.BaseModEntity
import kr.toxicity.model.api.platform.PlatformEntity
import kr.toxicity.model.api.platform.PlatformPlayer
import kr.toxicity.model.api.util.MathUtil.MINECRAFT_TICKS_PER_SECOND
import kr.toxicity.model.api.util.TransformedItemStack
import kr.toxicity.model.impl.fabric.*
import kr.toxicity.model.impl.fabric.chat.asAdventure
import net.kyori.adventure.text.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.FluidTags
import net.minecraft.util.Mth
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.Leashable
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.TamableAnimal
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.behavior.RamTarget
import net.minecraft.world.entity.ai.behavior.warden.SonicBoom
import net.minecraft.world.entity.ai.memory.MemoryModuleType
import net.minecraft.world.entity.ambient.Bat
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.animal.allay.Allay
import net.minecraft.world.entity.animal.axolotl.Axolotl
import net.minecraft.world.entity.animal.camel.Camel
import net.minecraft.world.entity.animal.dolphin.Dolphin
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse
import net.minecraft.world.entity.animal.equine.AbstractHorse
import net.minecraft.world.entity.animal.fox.Fox
import net.minecraft.world.entity.animal.frog.Frog
import net.minecraft.world.entity.animal.goat.Goat
import net.minecraft.world.entity.animal.golem.SnowGolem
import net.minecraft.world.entity.animal.panda.Panda
import net.minecraft.world.entity.animal.sheep.Sheep
import net.minecraft.world.entity.animal.sniffer.Sniffer
import net.minecraft.world.entity.animal.turtle.Turtle
import net.minecraft.world.entity.animal.wolf.Wolf
import net.minecraft.world.entity.boss.wither.WitherBoss
import net.minecraft.world.entity.monster.Creeper
import net.minecraft.world.entity.monster.ElderGuardian
import net.minecraft.world.entity.monster.Enderman
import net.minecraft.world.entity.monster.Ravager
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob
import net.minecraft.world.entity.monster.illager.AbstractIllager
import net.minecraft.world.entity.monster.illager.SpellcasterIllager
import net.minecraft.world.entity.monster.spider.Spider
import net.minecraft.world.entity.monster.warden.Warden
import net.minecraft.world.entity.monster.zombie.Zombie
import net.minecraft.world.phys.Vec3
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

    override fun updateTo(context: KeyframeRuntimeContext) {
        registerEntityQueries(context) { entity }
    }

    companion object {
        internal fun registerEntityQueries(context: KeyframeRuntimeContext, entity: () -> Entity) {
            // The evaluator binds this context under both query and q.
            context.setFlag("is_alive") { entity().isAlive }
            context.setFlag("is_on_ground") { entity().onGround() }
            context.setFlag("is_invisible") { entity().isInvisible }
            context.setFlag("is_silent") { entity().isSilent }
            context.setFlag("is_on_fire") { entity().isOnFire }
            context.setFlag("is_onfire") { entity().isOnFire }
            context.setFlag("is_fire_immune") { entity().fireImmune() }
            context.setFlag("is_in_lava") { entity().isInLava }
            context.setFlag("is_in_water") { entity().isInWater }
            context.setFlag("is_in_water_or_rain") { entity().isInWaterOrRain }
            context.setFlag("head_is_in_water") { entity().isEyeInFluid(FluidTags.WATER) }
            context.setFlag("is_swimming") { entity().isSwimming }
            context.setFlag("is_crawling") { entity().isVisuallyCrawling }
            context.setFlag("is_sneaking") { entity().isShiftKeyDown }
            context.setFlag("is_sprinting") { entity().isSprinting }
            context.setFlag("is_jumping") {
                when (val handle = entity()) {
                    is ServerPlayer -> handle.lastClientInput.jump()
                    is LivingEntity -> handle.isJumping
                    else -> false
                }
            }
            context.setFlag("is_wall_climbing") { (entity() as? Spider)?.isClimbing == true }
            context.setFlag("is_gliding") { (entity() as? LivingEntity)?.isFallFlying == true }
            context.setFlag("is_levitating") {
                (entity() as? LivingEntity)?.hasEffect(MobEffects.LEVITATION) == true
            }
            context.setFlag("has_collision") { !entity().noPhysics }
            context.setFlag("has_gravity") { entity().gravity != 0.0 }
            context.setFlag("is_moving") { entity().knownSpeed.lengthSqr() > 0.0 }
            context.setFlag("is_riding") { entity().isPassenger }
            context.setFlag("has_rider") { entity().isVehicle }
            context.setFlag("has_player_rider") { entity().passengers.any { it is ServerPlayer } }
            context.setFlag("is_leashed") { (entity() as? Leashable)?.isLeashed == true }
            context.setFlag("is_baby") { (entity() as? LivingEntity)?.isBaby == true }
            context.setFlag("is_in_love") { (entity() as? Animal)?.isInLove == true }
            context.setFlag("is_sleeping") { (entity() as? LivingEntity)?.isSleeping == true }
            context.setFlag("is_sitting") {
                when (val handle = entity()) {
                    is TamableAnimal -> handle.isInSittingPose
                    is Fox -> handle.isSitting
                    is Panda -> handle.isSitting
                    is Camel -> handle.hasPose(Pose.SITTING)
                    else -> false
                }
            }
            context.setFlag("is_tamed") {
                when (val handle = entity()) {
                    is TamableAnimal -> handle.isTame
                    is AbstractHorse -> handle.isTamed
                    else -> false
                }
            }
            context.setFlag("has_owner") {
                when (val handle = entity()) {
                    is TamableAnimal -> handle.ownerReference != null
                    is AbstractHorse -> handle.ownerReference != null
                    else -> false
                }
            }
            context.setFlag("is_chested") { (entity() as? AbstractChestedHorse)?.hasChest() == true }
            context.setFlag("is_saddled") { (entity() as? Mob)?.isSaddled == true }
            context.setFlag("is_charged") { (entity() as? Creeper)?.isPowered == true }
            context.setFlag("is_ignited") { (entity() as? Creeper)?.isIgnited == true }
            context.setFlag("is_sheared") {
                when (val handle = entity()) {
                    is Sheep -> handle.isSheared
                    is SnowGolem -> !handle.hasPumpkin()
                    else -> false
                }
            }
            context.setFlag("is_carrying_block") { (entity() as? Enderman)?.carriedBlock != null }
            context.setFlag("is_casting") { (entity() as? SpellcasterIllager)?.isCastingSpell == true }
            context.setFlag("is_elder") { entity() is ElderGuardian }
            context.setFlag("is_dancing") { (entity() as? Allay)?.isDancing == true }
            context.setFlag("is_playing_dead") { (entity() as? Axolotl)?.isPlayingDead == true }
            context.setFlag("is_resting") { (entity() as? Bat)?.isResting == true }
            context.setFlag("is_bribed") { (entity() as? Dolphin)?.gotFish() == true }
            context.setFlag("is_eating") { (entity() as? Panda)?.isEating == true }
            context.setFlag("is_rolling") { (entity() as? Panda)?.isRolling == true }
            context.setFlag("is_sneezing") { (entity() as? Panda)?.isSneezing == true }
            context.setFlag("is_interested") {
                when (val handle = entity()) {
                    is Fox -> handle.isInterested
                    is Wolf -> handle.isInterested
                    else -> false
                }
            }
            context.setFlag("is_illager_captain") { (entity() as? AbstractIllager)?.isPatrolLeader == true }
            context.setFlag("is_stunned") { ((entity() as? Ravager)?.stunnedTick ?: 0) > 0 }
            context.setFlag("is_shield_powered") { (entity() as? WitherBoss)?.isPowered == true }
            context.setFlag("is_transforming") { (entity() as? Zombie)?.isUnderWaterConverting == true }
            context.setFlag("is_pregnant") {
                when (val handle = entity()) {
                    is Turtle -> handle.hasEgg()
                    is Frog -> handle.brain.hasMemoryValue(MemoryModuleType.IS_PREGNANT)
                    else -> false
                }
            }
            context.setFlag("is_laying_egg") { (entity() as? Turtle)?.isLayingEgg == true }
            context.setFlag("is_croaking") { (entity() as? Frog)?.hasPose(Pose.CROAKING) == true }
            context.setFlag("is_sniffing") {
                when (val handle = entity()) {
                    // Synced state changes update these animation flags on the server too.
                    is Sniffer -> handle.sniffingAnimationState.isStarted
                    is Warden -> handle.hasPose(Pose.SNIFFING)
                    else -> false
                }
            }
            context.setFlag("is_digging") {
                when (val handle = entity()) {
                    is Sniffer -> handle.diggingAnimationState.isStarted
                    is Warden -> handle.hasPose(Pose.DIGGING)
                    else -> false
                }
            }
            context.setFlag("is_searching") { (entity() as? Sniffer)?.isSearching == true }
            context.setFlag("is_emerging") { (entity() as? Warden)?.hasPose(Pose.EMERGING) == true }
            context.setFlag("is_roaring") {
                when (val handle = entity()) {
                    is Ravager -> handle.roarTick > 0
                    is Warden -> handle.hasPose(Pose.ROARING)
                    else -> false
                }
            }
            context.setFlag("is_sonic_boom") {
                (entity() as? Warden)?.brain?.runningBehaviors?.any { it is SonicBoom } == true
            }
            context.setFlag("is_ram_attacking") {
                (entity() as? Goat)?.brain?.runningBehaviors?.any { it is RamTarget } == true
            }
            context.setFlag("has_target") { (entity() as? Mob)?.target != null }
            context.setFlag("blocking") { (entity() as? LivingEntity)?.isBlocking == true }
            context.setFlag("is_using_item") { (entity() as? LivingEntity)?.isUsingItem == true }
            context.setFlag("has_head_gear") {
                (entity() as? LivingEntity)?.hasItemInSlot(EquipmentSlot.HEAD) == true
            }
            context.setFunction("equipment_count") { ->
                val handle = entity() as? LivingEntity ?: return@setFunction 0F
                EquipmentSlot.VALUES.count { it.isArmor && handle.hasItemInSlot(it) }.toFloat()
            }
            context.setFunction("health") { -> (entity() as? LivingEntity)?.health ?: 0F }
            context.setFunction("max_health") { -> (entity() as? LivingEntity)?.maxHealth ?: 0F }
            context.setFunction("death_ticks") { -> (entity() as? LivingEntity)?.deathTime?.toFloat() ?: 0F }
            context.setFunction("hurt_time") { -> (entity() as? LivingEntity)?.hurtTime?.toFloat() ?: 0F }
            context.setFunction("item_in_use_duration") { ->
                val ticks = (entity() as? LivingEntity)?.ticksUsingItem ?: 0
                ticks.toFloat() / MINECRAFT_TICKS_PER_SECOND
            }
            context.setFunction("model_scale") { -> (entity() as? LivingEntity)?.scale ?: 1F }
            context.setFunction("body_y_rotation") { ->
                entity().bodyYawDegrees()
            }
            context.setFunction("ride_body_y_rotation") { ->
                entity().vehicle?.bodyYawDegrees() ?: 0F
            }
            context.setFunction("position") { axis -> entity().position().component(axis) }
            context.setFunction("position_delta") { axis -> entity().knownSpeed.component(axis) }
            context.setFunction("movement_direction") { axis -> entity().knownSpeed.normalize().component(axis) }
            context.setFunction("head_x_rotation") { index ->
                when (val handle = entity()) {
                    is LivingEntity -> if (index == 0F) handle.xRot else {
                        (handle as? WitherBoss)?.headXRots?.let { head ->
                            when (index) {
                                1F -> head[0]
                                2F -> head[1]
                                else -> 0F
                            }
                        } ?: 0F
                    }
                    else -> 0F
                }
            }
            context.setFunction("head_y_rotation") { index ->
                val handle = entity() as? LivingEntity ?: return@setFunction 0F
                // Equine queries require a separate clamp argument.
                if (handle is AbstractHorse) return@setFunction 0F
                val yaw = when (index) {
                    0F -> handle.yHeadRot
                    1F -> (handle as? WitherBoss)?.headYRots?.get(0) ?: return@setFunction 0F
                    2F -> (handle as? WitherBoss)?.headYRots?.get(1) ?: return@setFunction 0F
                    else -> return@setFunction 0F
                }
                // Bedrock head yaw winds opposite to body yaw in model expressions.
                Mth.wrapDegrees(-yaw)
            }
            context.setFunction("rider_body_y_rotation") { index ->
                entity().rider(index)?.bodyYawDegrees() ?: 0F
            }
            context.setFunction("rider_head_x_rotation") { index ->
                (entity().rider(index) as? LivingEntity)?.xRot ?: 0F
            }
            context.setFunction("rider_head_y_rotation") { index ->
                val rider = entity().rider(index) as? LivingEntity ?: return@setFunction 0F
                if (rider is AbstractHorse) 0F else Mth.wrapDegrees(rider.yHeadRot)
            }
            // Known speed is position change in blocks per tick, including client-controlled movement.
            context.setFunction("ground_speed") { ->
                (entity().knownSpeed.horizontalDistance() * MINECRAFT_TICKS_PER_SECOND).toFloat()
            }
            context.setFunction("vertical_speed") { ->
                (entity().knownSpeed.y * MINECRAFT_TICKS_PER_SECOND).toFloat()
            }
            context.setFunction("current_squish_value") { -> (entity() as? AbstractCubeMob)?.squish ?: 0F }
            context.setFunction("previous_squish_value") { -> (entity() as? AbstractCubeMob)?.oSquish ?: 0F }
            context.setFunction("swell_amount") { -> (entity() as? Creeper)?.getSwelling(1F) ?: 0F }
            context.setFunction("swelling_dir") { -> (entity() as? Creeper)?.swellDir?.toFloat() ?: 0F }
            context.setFunction("head_roll_angle") { ->
                ((entity() as? Wolf)?.getHeadRollAngle(1F) ?: 0F) * Mth.RAD_TO_DEG
            }
            context.setFunction("tail_angle") { ->
                ((entity() as? Wolf)?.tailAngle ?: 0F) * Mth.RAD_TO_DEG
            }
            context.setFunction("sit_amount") { -> (entity() as? Panda)?.getSitAmount(1F) ?: 0F }
            context.setFunction("lie_amount") { -> (entity() as? Panda)?.getLieOnBackAmount(1F) ?: 0F }
            context.setFunction("roll_counter") { -> (entity() as? Panda)?.rollCounter?.toFloat() ?: 0F }
        }

        private fun Entity.bodyYawDegrees(): Float = Mth.wrapDegrees(if (this is LivingEntity) yBodyRot else yRot)

        private fun Vec3.component(axis: Float): Float = when (axis) {
            0F -> x.toFloat()
            1F -> y.toFloat()
            2F -> z.toFloat()
            else -> 0F
        }

        private fun Entity.rider(index: Float): Entity? {
            val slot = index.toInt()
            return if (index == slot.toFloat()) passengers.getOrNull(slot) else null
        }

        private inline fun KeyframeRuntimeContext.setFlag(key: String, crossinline state: () -> Boolean) {
            setFunction(key) { -> if (state()) 1F else 0F }
        }
    }
}
