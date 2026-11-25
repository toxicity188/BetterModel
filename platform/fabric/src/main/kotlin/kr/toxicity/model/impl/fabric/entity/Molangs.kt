/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.impl.fabric.entity

import kr.toxicity.model.api.animation.keyframe.KeyframeRuntimeContext
import kr.toxicity.model.api.animation.keyframe.KeyframeRuntimeContext.FloatFunction
import kr.toxicity.model.api.animation.keyframe.KeyframeRuntimeContext.FloatFunction0
import kr.toxicity.model.api.animation.keyframe.KeyframeRuntimeContext.FloatFunction1
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
import net.minecraft.world.entity.TamableAnimal
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

private val entityQueries: Map<String, (() -> Entity) -> FloatFunction> = mapOf(
    // The evaluator binds this context under both query and q.
    "is_alive" to { entity -> flag { entity().isAlive } },
    "is_on_ground" to { entity -> flag { entity().onGround() } },
    "is_invisible" to { entity -> flag { entity().isInvisible } },
    "is_silent" to { entity -> flag { entity().isSilent } },
    "is_on_fire" to { entity -> flag { entity().isOnFire } },
    "is_onfire" to { entity -> flag { entity().isOnFire } },
    "is_fire_immune" to { entity -> flag { entity().fireImmune() } },
    "is_in_lava" to { entity -> flag { entity().isInLava } },
    "is_in_water" to { entity -> flag { entity().isInWater } },
    "is_in_water_or_rain" to { entity -> flag { entity().isInWaterOrRain } },
    "head_is_in_water" to { entity -> flag { entity().isEyeInFluid(FluidTags.WATER) } },
    "is_swimming" to { entity -> flag { entity().isSwimming } },
    "is_crawling" to { entity -> flag { entity().isVisuallyCrawling } },
    "is_sneaking" to { entity -> flag { entity().isShiftKeyDown } },
    "is_sprinting" to { entity -> flag { entity().isSprinting } },
    "is_jumping" to { entity ->
        flag {
        when (val handle = entity()) {
            is ServerPlayer -> handle.lastClientInput.jump()
            is LivingEntity -> handle.isJumping
            else -> false
        }
        }
    },
    "is_wall_climbing" to { entity -> flag { (entity() as? Spider)?.isClimbing == true } },
    "is_gliding" to { entity -> flag { (entity() as? LivingEntity)?.isFallFlying == true } },
    "is_levitating" to { entity ->
        flag {
        (entity() as? LivingEntity)?.hasEffect(MobEffects.LEVITATION) == true
        }
    },
    "has_collision" to { entity -> flag { !entity().noPhysics } },
    "has_gravity" to { entity -> flag { entity().gravity != 0.0 } },
    "is_moving" to { entity -> flag { entity().knownSpeed.lengthSqr() > 0.0 } },
    "is_riding" to { entity -> flag { entity().isPassenger } },
    "has_rider" to { entity -> flag { entity().isVehicle } },
    "has_player_rider" to { entity -> flag { entity().passengers.any { it is ServerPlayer } } },
    "is_leashed" to { entity -> flag { (entity() as? Leashable)?.isLeashed == true } },
    "is_baby" to { entity -> flag { (entity() as? LivingEntity)?.isBaby == true } },
    "is_in_love" to { entity -> flag { (entity() as? Animal)?.isInLove == true } },
    "is_sleeping" to { entity -> flag { (entity() as? LivingEntity)?.isSleeping == true } },
    "is_sitting" to { entity ->
        flag {
        when (val handle = entity()) {
            is TamableAnimal -> handle.isInSittingPose
            is Fox -> handle.isSitting
            is Panda -> handle.isSitting
            is Camel -> handle.hasPose(Pose.SITTING)
            else -> false
        }
        }
    },
    "is_tamed" to { entity ->
        flag {
        when (val handle = entity()) {
            is TamableAnimal -> handle.isTame
            is AbstractHorse -> handle.isTamed
            else -> false
        }
        }
    },
    "has_owner" to { entity ->
        flag {
        when (val handle = entity()) {
            is TamableAnimal -> handle.ownerReference != null
            is AbstractHorse -> handle.ownerReference != null
            else -> false
        }
        }
    },
    "is_chested" to { entity -> flag { (entity() as? AbstractChestedHorse)?.hasChest() == true } },
    "is_saddled" to { entity -> flag { (entity() as? Mob)?.isSaddled == true } },
    "is_charged" to { entity -> flag { (entity() as? Creeper)?.isPowered == true } },
    "is_ignited" to { entity -> flag { (entity() as? Creeper)?.isIgnited == true } },
    "is_sheared" to { entity ->
        flag {
        when (val handle = entity()) {
            is Sheep -> handle.isSheared
            is SnowGolem -> !handle.hasPumpkin()
            else -> false
        }
        }
    },
    "is_carrying_block" to { entity -> flag { (entity() as? Enderman)?.carriedBlock != null } },
    "is_casting" to { entity -> flag { (entity() as? SpellcasterIllager)?.isCastingSpell == true } },
    "is_elder" to { entity -> flag { entity() is ElderGuardian } },
    "is_dancing" to { entity -> flag { (entity() as? Allay)?.isDancing == true } },
    "is_playing_dead" to { entity -> flag { (entity() as? Axolotl)?.isPlayingDead == true } },
    "is_resting" to { entity -> flag { (entity() as? Bat)?.isResting == true } },
    "is_bribed" to { entity -> flag { (entity() as? Dolphin)?.gotFish() == true } },
    "is_eating" to { entity -> flag { (entity() as? Panda)?.isEating == true } },
    "is_rolling" to { entity -> flag { (entity() as? Panda)?.isRolling == true } },
    "is_sneezing" to { entity -> flag { (entity() as? Panda)?.isSneezing == true } },
    "is_interested" to { entity ->
        flag {
        when (val handle = entity()) {
            is Fox -> handle.isInterested
            is Wolf -> handle.isInterested
            else -> false
        }
        }
    },
    "is_illager_captain" to { entity -> flag { (entity() as? AbstractIllager)?.isPatrolLeader == true } },
    "is_stunned" to { entity -> flag { ((entity() as? Ravager)?.stunnedTick ?: 0) > 0 } },
    "is_shield_powered" to { entity -> flag { (entity() as? WitherBoss)?.isPowered == true } },
    "is_transforming" to { entity -> flag { (entity() as? Zombie)?.isUnderWaterConverting == true } },
    "is_pregnant" to { entity ->
        flag {
        when (val handle = entity()) {
            is Turtle -> handle.hasEgg()
            is Frog -> handle.brain.hasMemoryValue(MemoryModuleType.IS_PREGNANT)
            else -> false
        }
        }
    },
    "is_laying_egg" to { entity -> flag { (entity() as? Turtle)?.isLayingEgg == true } },
    "is_croaking" to { entity -> flag { (entity() as? Frog)?.hasPose(Pose.CROAKING) == true } },
    "is_sniffing" to { entity ->
        flag {
        when (val handle = entity()) {
            // Synced state changes update these animation flags on the server too.
            is Sniffer -> handle.sniffingAnimationState.isStarted
            is Warden -> handle.hasPose(Pose.SNIFFING)
            else -> false
        }
        }
    },
    "is_digging" to { entity ->
        flag {
        when (val handle = entity()) {
            is Sniffer -> handle.diggingAnimationState.isStarted
            is Warden -> handle.hasPose(Pose.DIGGING)
            else -> false
        }
        }
    },
    "is_searching" to { entity -> flag { (entity() as? Sniffer)?.isSearching == true } },
    "is_emerging" to { entity -> flag { (entity() as? Warden)?.hasPose(Pose.EMERGING) == true } },
    "is_roaring" to { entity ->
        flag {
        when (val handle = entity()) {
            is Ravager -> handle.roarTick > 0
            is Warden -> handle.hasPose(Pose.ROARING)
            else -> false
        }
        }
    },
    // Sonic boom has no equivalent server-side running flag.
    // context.setFlag("is_sonic_boom") {
    //     (entity() as? Warden)?.brain?.runningBehaviors?.any { it is SonicBoom } == true
    // }
    "is_ram_attacking" to { entity ->
        flag {
        (entity() as? Goat)?.brain?.hasMemoryValue(MemoryModuleType.RAM_TARGET) == true
        }
    },
    "has_target" to { entity -> flag { (entity() as? Mob)?.target != null } },
    "blocking" to { entity -> flag { (entity() as? LivingEntity)?.isBlocking == true } },
    "is_using_item" to { entity -> flag { (entity() as? LivingEntity)?.isUsingItem == true } },
    "has_head_gear" to { entity ->
        flag {
        (entity() as? LivingEntity)?.hasItemInSlot(EquipmentSlot.HEAD) == true
        }
    },
    "equipment_count" to { entity ->
        FloatFunction0 {
        val handle = entity() as? LivingEntity ?: return@FloatFunction0 0F
        EquipmentSlot.VALUES.count { it.isArmor && handle.hasItemInSlot(it) }.toFloat()
        }
    },
    "health" to { entity -> FloatFunction0 { (entity() as? LivingEntity)?.health ?: 0F } },
    "max_health" to { entity -> FloatFunction0 { (entity() as? LivingEntity)?.maxHealth ?: 0F } },
    "death_ticks" to { entity -> FloatFunction0 { (entity() as? LivingEntity)?.deathTime?.toFloat() ?: 0F } },
    "hurt_time" to { entity -> FloatFunction0 { (entity() as? LivingEntity)?.hurtTime?.toFloat() ?: 0F } },
    "item_in_use_duration" to { entity ->
        FloatFunction0 {
        val ticks = (entity() as? LivingEntity)?.ticksUsingItem ?: 0
        ticks.toFloat() / MINECRAFT_TICKS_PER_SECOND
        }
    },
    "model_scale" to { entity -> FloatFunction0 { (entity() as? LivingEntity)?.scale ?: 1F } },
    "body_y_rotation" to { entity ->
        FloatFunction0 {
        entity().bodyYawDegrees()
        }
    },
    "ride_body_y_rotation" to { entity ->
        FloatFunction0 {
        entity().vehicle?.bodyYawDegrees() ?: 0F
        }
    },
    "position" to { entity -> FloatFunction1 { axis -> entity().position().component(axis) } },
    "position_delta" to { entity -> FloatFunction1 { axis -> entity().knownSpeed.component(axis) } },
    "movement_direction" to { entity -> FloatFunction1 { axis -> entity().knownSpeed.normalize().component(axis) } },
    "head_x_rotation" to { entity ->
        FloatFunction1 { index ->
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
    },
    "head_y_rotation" to { entity ->
        FloatFunction1 { index ->
        val handle = entity() as? LivingEntity ?: return@FloatFunction1 0F
        // Equine queries require a separate clamp argument.
        if (handle is AbstractHorse) return@FloatFunction1 0F
        val yaw = when (index) {
            0F -> handle.yHeadRot
            1F -> (handle as? WitherBoss)?.headYRots?.get(0) ?: return@FloatFunction1 0F
            2F -> (handle as? WitherBoss)?.headYRots?.get(1) ?: return@FloatFunction1 0F
            else -> return@FloatFunction1 0F
        }
        // Bedrock head yaw winds opposite to body yaw in model expressions.
        Mth.wrapDegrees(-yaw)
        }
    },
    "rider_body_y_rotation" to { entity ->
        FloatFunction1 { index ->
        entity().rider(index)?.bodyYawDegrees() ?: 0F
        }
    },
    "rider_head_x_rotation" to { entity ->
        FloatFunction1 { index ->
        (entity().rider(index) as? LivingEntity)?.xRot ?: 0F
        }
    },
    "rider_head_y_rotation" to { entity ->
        FloatFunction1 { index ->
        val rider = entity().rider(index) as? LivingEntity ?: return@FloatFunction1 0F
        if (rider is AbstractHorse) 0F else Mth.wrapDegrees(rider.yHeadRot)
        }
    },
    // Known speed is position change in blocks per tick, including client-controlled movement.
    "ground_speed" to { entity ->
        FloatFunction0 {
        (entity().knownSpeed.horizontalDistance() * MINECRAFT_TICKS_PER_SECOND).toFloat()
        }
    },
    "vertical_speed" to { entity ->
        FloatFunction0 {
        (entity().knownSpeed.y * MINECRAFT_TICKS_PER_SECOND).toFloat()
        }
    },
    "current_squish_value" to { entity -> FloatFunction0 { (entity() as? AbstractCubeMob)?.squish ?: 0F } },
    "previous_squish_value" to { entity -> FloatFunction0 { (entity() as? AbstractCubeMob)?.oSquish ?: 0F } },
    "swell_amount" to { entity -> FloatFunction0 { (entity() as? Creeper)?.getSwelling(1F) ?: 0F } },
    "swelling_dir" to { entity -> FloatFunction0 { (entity() as? Creeper)?.swellDir?.toFloat() ?: 0F } },
    "head_roll_angle" to { entity ->
        FloatFunction0 {
        ((entity() as? Wolf)?.getHeadRollAngle(1F) ?: 0F) * Mth.RAD_TO_DEG
        }
    },
    "tail_angle" to { entity ->
        FloatFunction0 {
        ((entity() as? Wolf)?.tailAngle ?: 0F) * Mth.RAD_TO_DEG
        }
    },
    "sit_amount" to { entity -> FloatFunction0 { (entity() as? Panda)?.getSitAmount(1F) ?: 0F } },
    "lie_amount" to { entity -> FloatFunction0 { (entity() as? Panda)?.getLieOnBackAmount(1F) ?: 0F } },
    "roll_counter" to { entity -> FloatFunction0 { (entity() as? Panda)?.rollCounter?.toFloat() ?: 0F } },
)

private val playerQueries: Map<String, (() -> ServerPlayer) -> FloatFunction> = mapOf(
    "is_spectator" to { player -> FloatFunction0 { if (player().isSpectator) 1F else 0F } },
    "player_level" to { player -> FloatFunction0 { player().experienceLevel.toFloat() } },
    "cardinal_facing" to { player -> FloatFunction0 { player().nearestViewDirection.get3DDataValue().toFloat() } },
    "cardinal_player_facing" to { player -> FloatFunction0 { player().nearestViewDirection.get3DDataValue().toFloat() } },
    "cardinal_facing_2d" to { player -> FloatFunction0 { player().direction.get3DDataValue().toFloat() } },
)

internal fun registerEntityQueries(context: KeyframeRuntimeContext, entity: () -> Entity) {
    entityQueries.forEach { (key, factory) -> context.set(key, factory(entity)) }
}

internal fun registerPlayerQueries(context: KeyframeRuntimeContext, player: () -> ServerPlayer) {
    registerEntityQueries(context, player)
    playerQueries.forEach { (key, factory) -> context.set(key, factory(player)) }
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


private inline fun flag(crossinline state: () -> Boolean): FloatFunction0 =
    FloatFunction0 { if (state()) 1F else 0F }
