/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.bukkit.nms.v26_R3

import kr.toxicity.model.api.animation.keyframe.KeyframeRuntimeContext
import kr.toxicity.model.api.bukkit.entity.BaseBukkitEntity
import kr.toxicity.model.api.platform.PlatformEntity
import kr.toxicity.model.api.platform.PlatformLocation
import kr.toxicity.model.api.platform.PlatformPlayer
import kr.toxicity.model.api.util.MathUtil.MINECRAFT_TICKS_PER_SECOND
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
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.behavior.RamTarget
import net.minecraft.world.entity.ai.behavior.warden.SonicBoom
import net.minecraft.world.entity.ai.memory.MemoryModuleType
import net.minecraft.world.entity.ambient.Bat
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.animal.allay.Allay
import net.minecraft.world.entity.animal.axolotl.Axolotl
import net.minecraft.world.entity.animal.dolphin.Dolphin
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
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.entity.ChestedHorse
import org.bukkit.entity.Sittable
import org.bukkit.entity.Tameable
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
    override fun bodyYaw(): Float = handle().let { if (it is LivingEntity) it.yBodyRot else it.yRot }
    override fun yaw(): Float = handle().yRot
    override fun headYaw(): Float = handle().let { if (it is LivingEntity) it.yHeadRot else it.yRot }
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

    override fun passengerPosition(dest: Vector3f): Vector3f {
        return handle().passengerPosition(dest)
    }

    override fun platform(): PlatformEntity = delegate.wrap()
    override fun trackedBy(): Stream<PlatformPlayer> = delegate.trackedBy.stream().map { it.wrap() }
    override fun location(): PlatformLocation = delegate.location.wrap()

    override fun updateTo(context: KeyframeRuntimeContext) {
        registerEntityQueries(delegate, context)
    }

    companion object {
        fun registerEntityQueries(entity: CraftEntity, context: KeyframeRuntimeContext) {
            // The evaluator binds this context under both query and q.
            context.setFlag("is_alive") { entity.vanillaEntity.isAlive }
            context.setFlag("is_on_ground") { entity.vanillaEntity.onGround() }
            context.setFlag("is_invisible") { entity.vanillaEntity.isInvisible }
            context.setFlag("is_silent") { entity.vanillaEntity.isSilent }
            context.setFlag("is_on_fire") { entity.vanillaEntity.isOnFire }
            context.setFlag("is_onfire") { entity.vanillaEntity.isOnFire }
            context.setFlag("is_fire_immune") { entity.vanillaEntity.fireImmune() }
            context.setFlag("is_in_lava") { entity.vanillaEntity.isInLava }
            context.setFlag("is_in_water") { entity.vanillaEntity.isInWater }
            context.setFlag("is_in_water_or_rain") { entity.vanillaEntity.isInWaterOrRain }
            context.setFlag("head_is_in_water") { entity.vanillaEntity.isEyeInFluid(FluidTags.WATER) }
            context.setFlag("is_swimming") { entity.vanillaEntity.isSwimming }
            context.setFlag("is_crawling") { entity.vanillaEntity.isVisuallyCrawling }
            context.setFlag("is_sneaking") { entity.vanillaEntity.isShiftKeyDown }
            context.setFlag("is_sprinting") { entity.vanillaEntity.isSprinting }
            context.setFlag("is_jumping") {
                when (val handle = entity.vanillaEntity) {
                    is ServerPlayer -> handle.lastClientInput.jump()
                    is LivingEntity -> handle.isJumping
                    else -> false
                }
            }
            context.setFlag("is_wall_climbing") { (entity.vanillaEntity as? Spider)?.isClimbing == true }
            context.setFlag("is_gliding") { (entity.vanillaEntity as? LivingEntity)?.isFallFlying == true }
            context.setFlag("is_levitating") {
                (entity.vanillaEntity as? LivingEntity)?.hasEffect(MobEffects.LEVITATION) == true
            }
            context.setFlag("has_collision") { !entity.vanillaEntity.noPhysics }
            context.setFlag("has_gravity") { entity.vanillaEntity.gravity != 0.0 }
            context.setFlag("is_moving") { entity.vanillaEntity.knownSpeed.lengthSqr() > 0.0 }
            context.setFlag("is_riding") { entity.vanillaEntity.isPassenger }
            context.setFlag("has_rider") { entity.vanillaEntity.isVehicle }
            context.setFlag("has_player_rider") { entity.vanillaEntity.passengers.any { it is ServerPlayer } }
            context.setFlag("is_leashed") { (entity.vanillaEntity as? Leashable)?.isLeashed == true }
            context.setFlag("is_baby") { (entity.vanillaEntity as? LivingEntity)?.isBaby == true }
            context.setFlag("is_in_love") { (entity.vanillaEntity as? Animal)?.isInLove == true }
            context.setFlag("is_sleeping") { (entity.vanillaEntity as? LivingEntity)?.isSleeping == true }
            context.setFlag("is_sitting") { (entity as? Sittable)?.isSitting == true }
            context.setFlag("is_tamed") { (entity as? Tameable)?.isTamed == true }
            context.setFlag("has_owner") { (entity as? Tameable)?.ownerUniqueId != null }
            context.setFlag("is_chested") { (entity as? ChestedHorse)?.isCarryingChest == true }
            context.setFlag("is_saddled") { (entity.vanillaEntity as? Mob)?.isSaddled == true }
            context.setFlag("is_charged") { (entity.vanillaEntity as? Creeper)?.isPowered == true }
            context.setFlag("is_ignited") { (entity.vanillaEntity as? Creeper)?.isIgnited == true }
            context.setFlag("is_sheared") {
                when (val handle = entity.vanillaEntity) {
                    is Sheep -> handle.isSheared
                    is SnowGolem -> !handle.hasPumpkin()
                    else -> false
                }
            }
            context.setFlag("is_carrying_block") { (entity.vanillaEntity as? Enderman)?.carriedBlock != null }
            context.setFlag("is_casting") { (entity.vanillaEntity as? SpellcasterIllager)?.isCastingSpell == true }
            context.setFlag("is_elder") { entity.vanillaEntity is ElderGuardian }
            context.setFlag("is_dancing") { (entity.vanillaEntity as? Allay)?.isDancing == true }
            context.setFlag("is_playing_dead") { (entity.vanillaEntity as? Axolotl)?.isPlayingDead == true }
            context.setFlag("is_resting") { (entity.vanillaEntity as? Bat)?.isResting == true }
            context.setFlag("is_bribed") { (entity.vanillaEntity as? Dolphin)?.gotFish() == true }
            context.setFlag("is_eating") { (entity.vanillaEntity as? Panda)?.isEating == true }
            context.setFlag("is_rolling") { (entity.vanillaEntity as? Panda)?.isRolling == true }
            context.setFlag("is_sneezing") { (entity.vanillaEntity as? Panda)?.isSneezing == true }
            context.setFlag("is_interested") {
                when (val handle = entity.vanillaEntity) {
                    is Fox -> handle.isInterested
                    is Wolf -> handle.isInterested
                    else -> false
                }
            }
            context.setFlag("is_illager_captain") { (entity.vanillaEntity as? AbstractIllager)?.isPatrolLeader == true }
            context.setFlag("is_stunned") { ((entity.vanillaEntity as? Ravager)?.stunnedTick ?: 0) > 0 }
            context.setFlag("is_shield_powered") { (entity.vanillaEntity as? WitherBoss)?.isPowered == true }
            context.setFlag("is_transforming") { (entity.vanillaEntity as? Zombie)?.isUnderWaterConverting == true }
            context.setFlag("is_pregnant") {
                when (val handle = entity.vanillaEntity) {
                    is Turtle -> handle.hasEgg()
                    is Frog -> handle.brain.hasMemoryValue(MemoryModuleType.IS_PREGNANT)
                    else -> false
                }
            }
            context.setFlag("is_laying_egg") { (entity.vanillaEntity as? Turtle)?.isLayingEgg == true }
            context.setFlag("is_croaking") { (entity.vanillaEntity as? Frog)?.hasPose(Pose.CROAKING) == true }
            context.setFlag("is_sniffing") {
                when (val handle = entity.vanillaEntity) {
                    is Sniffer -> handle.state == Sniffer.State.SNIFFING
                    is Warden -> handle.hasPose(Pose.SNIFFING)
                    else -> false
                }
            }
            context.setFlag("is_digging") {
                when (val handle = entity.vanillaEntity) {
                    is Sniffer -> handle.state == Sniffer.State.DIGGING
                    is Warden -> handle.hasPose(Pose.DIGGING)
                    else -> false
                }
            }
            context.setFlag("is_searching") { (entity.vanillaEntity as? Sniffer)?.state == Sniffer.State.SEARCHING }
            context.setFlag("is_emerging") { (entity.vanillaEntity as? Warden)?.hasPose(Pose.EMERGING) == true }
            context.setFlag("is_roaring") {
                when (val handle = entity.vanillaEntity) {
                    is Ravager -> handle.roarTick > 0
                    is Warden -> handle.hasPose(Pose.ROARING)
                    else -> false
                }
            }
            context.setFlag("is_sonic_boom") {
                (entity.vanillaEntity as? Warden)?.brain?.runningBehaviors?.any { it is SonicBoom } == true
            }
            context.setFlag("is_ram_attacking") {
                (entity.vanillaEntity as? Goat)?.brain?.runningBehaviors?.any { it is RamTarget } == true
            }
            context.setFlag("has_target") { (entity.vanillaEntity as? Mob)?.target != null }
            context.setFlag("blocking") { (entity.vanillaEntity as? LivingEntity)?.isBlocking == true }
            context.setFlag("is_using_item") { (entity.vanillaEntity as? LivingEntity)?.isUsingItem == true }
            context.setFlag("has_head_gear") {
                (entity.vanillaEntity as? LivingEntity)?.hasItemInSlot(EquipmentSlot.HEAD) == true
            }
            context.setFunction("equipment_count") {
                val handle = entity.vanillaEntity as? LivingEntity ?: return@setFunction 0F
                EquipmentSlot.VALUES.count { it.isArmor && handle.hasItemInSlot(it) }.toFloat()
            }
            context.setFunction("health") { (entity.vanillaEntity as? LivingEntity)?.health ?: 0F }
            context.setFunction("max_health") { (entity.vanillaEntity as? LivingEntity)?.maxHealth ?: 0F }
            context.setFunction("death_ticks") { (entity.vanillaEntity as? LivingEntity)?.deathTime?.toFloat() ?: 0F }
            context.setFunction("hurt_time") { (entity.vanillaEntity as? LivingEntity)?.hurtTime?.toFloat() ?: 0F }
            context.setFunction("item_in_use_duration") {
                val ticks = (entity.vanillaEntity as? LivingEntity)?.ticksUsingItem ?: 0
                ticks.toFloat() / MINECRAFT_TICKS_PER_SECOND
            }
            context.setFunction("model_scale") { (entity.vanillaEntity as? LivingEntity)?.scale ?: 1F }
            context.setFunction("body_y_rotation") {
                entity.vanillaEntity.bodyYawDegrees()
            }
            context.setFunction("ride_body_y_rotation") {
                entity.vanillaEntity.vehicle?.bodyYawDegrees() ?: 0F
            }
            // Known speed is position change in blocks per tick, including client-controlled movement.
            context.setFunction("ground_speed") {
                (entity.vanillaEntity.knownSpeed.horizontalDistance() * MINECRAFT_TICKS_PER_SECOND).toFloat()
            }
            context.setFunction("vertical_speed") {
                (entity.vanillaEntity.knownSpeed.y * MINECRAFT_TICKS_PER_SECOND).toFloat()
            }
            context.setFunction("current_squish_value") { (entity.vanillaEntity as? AbstractCubeMob)?.squish ?: 0F }
            context.setFunction("previous_squish_value") { (entity.vanillaEntity as? AbstractCubeMob)?.oSquish ?: 0F }
            context.setFunction("swell_amount") { (entity.vanillaEntity as? Creeper)?.getSwelling(1F) ?: 0F }
            context.setFunction("swelling_dir") { (entity.vanillaEntity as? Creeper)?.swellDir?.toFloat() ?: 0F }
            context.setFunction("head_roll_angle") {
                ((entity.vanillaEntity as? Wolf)?.getHeadRollAngle(1F) ?: 0F) * Mth.RAD_TO_DEG
            }
            context.setFunction("tail_angle") {
                ((entity.vanillaEntity as? Wolf)?.tailAngle ?: 0F) * Mth.RAD_TO_DEG
            }
            context.setFunction("sit_amount") { (entity.vanillaEntity as? Panda)?.getSitAmount(1F) ?: 0F }
            context.setFunction("lie_amount") { (entity.vanillaEntity as? Panda)?.getLieOnBackAmount(1F) ?: 0F }
            context.setFunction("roll_counter") { (entity.vanillaEntity as? Panda)?.rollCounter?.toFloat() ?: 0F }
        }

        private fun Entity.bodyYawDegrees(): Float = Mth.wrapDegrees(if (this is LivingEntity) yBodyRot else yRot)

        private inline fun KeyframeRuntimeContext.setFlag(key: String, crossinline state: () -> Boolean) {
            setFunction(key) { if (state()) 1F else 0F }
        }
    }
}
