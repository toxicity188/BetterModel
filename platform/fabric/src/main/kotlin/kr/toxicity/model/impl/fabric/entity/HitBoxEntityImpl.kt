/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.impl.fabric.entity

import kr.toxicity.model.api.bone.BoneMovement
import kr.toxicity.model.api.bone.RenderedBone
import kr.toxicity.model.api.config.DebugConfig
import kr.toxicity.model.api.data.blueprint.ModelBoundingBox
import kr.toxicity.model.api.event.hitbox.*
import kr.toxicity.model.api.mount.MountController
import kr.toxicity.model.api.nms.HitBox
import kr.toxicity.model.api.nms.HitBoxListener
import kr.toxicity.model.api.nms.ModelInteractionHand
import kr.toxicity.model.api.platform.PlatformEntity
import kr.toxicity.model.api.platform.PlatformPlayer
import kr.toxicity.model.impl.fabric.*
import kr.toxicity.model.impl.fabric.world.damagesource.ModelDamageSourceImpl
import kr.toxicity.model.util.CONFIG
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.entity.projectile.ProjectileDeflection
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import java.awt.Color
import java.util.*

class HitBoxEntityImpl(
    private val source: ModelBoundingBox,
    private val bone: RenderedBone,
    private var listener: HitBoxListener,
    private val delegate: Entity,
    private var mountController: MountController
) :
    AbstractArmorStand(EntityTypes.ARMOR_STAND, delegate.level()),
    HitBox
{
    private val posCache = BoneMovement()
    private var initialized = false
    private var jumpDelay = 0
    private var mounted = false
    private var noGravity = if (delegate is Mob) delegate.isNoAi else delegate.isNoGravity
    private var forceDismount = false
    private var onFly = false

    fun calculateDimensions(): EntityDimensions {
        val width = (source.x() + source.z()) * 0.5
        val height = source.y()

        return EntityDimensions(
            width.toFloat(),
            height.toFloat(),
            delegate.eyeHeight,
            EntityAttachments.createDefault(0F, 0F),
            false
        ).scale(
            bone.hitBoxScale()
        )
    }

    private val interaction by lazy {
        InteractionEntityImpl(this)
    }

    private val applier = InsideBlockEffectApplier.StepBasedCollector()

    init {
        snapTo(delegate.position())
        isInvisible = true
        isSilent = true
        initialized = true
        level().addFreshEntity(this)

        interaction.snapTo(delegate.position())
        interaction.startRiding(this)
        level().addFreshEntity(interaction)
        listener.handle(HitBoxCreateEvent(this))
    }

    private fun initialSetup() {
        if (mounted) {
            mounted = false
            if (delegate is Mob) delegate.isNoAi = noGravity
            else delegate.isNoGravity = noGravity
        }
    }

    override fun id(): Int = id

    override fun uuid(): UUID = uuid

    override fun source(): PlatformEntity = delegate.wrap()

    override fun positionSource(): RenderedBone = bone

    override fun forceDismount(): Boolean = forceDismount

    override fun mountController(): MountController = mountController

    override fun hasMountDriver(): Boolean = controllingPassenger != null

    override fun mountController(controller: MountController) {
        this.mountController = controller
    }

    override fun relativePosition(): Vector3f {
        return bone.hitBoxPosition(posCache).add(
            delegate.x.toFloat(),
            delegate.y.toFloat(),
            delegate.z.toFloat()
        )
    }

    override fun listener(): HitBoxListener = listener

    override fun listener(listener: HitBoxListener) {
        this.listener = listener
    }

    override fun getItemBySlot(slot: EquipmentSlot): ItemStack = ItemStack.EMPTY

    override fun setItemSlot(slot: EquipmentSlot, stack: ItemStack) = Unit

    override fun getMainArm(): HumanoidArm = HumanoidArm.RIGHT

    override fun mount(entity: PlatformEntity) {
        if (controllingPassenger != null) {
            return
        }

        entity.unwarp().startRiding(this, true, true)
        if (mountController.canControl()) {
            mounted = true
            noGravity = delegate.isNoGravity
        }

        listener.handle(HitBoxMountEvent(this, entity))
    }

    override fun dismount(entity: PlatformEntity) {
        forceDismount = true

        entity.unwarp().stopRiding()
        listener.handle(HitBoxDismountEvent(this, entity))

        forceDismount = false
    }

    override fun dismountAll() {
        forceDismount = true

        interaction.passengers.forEach { passenger ->
            passenger.stopRiding()
            listener.handle(HitBoxDismountEvent(this, passenger.wrap()))
        }

        forceDismount = false
    }

    override fun setRemainingFireTicks(remainingFireTicks: Int) {
        delegate.remainingFireTicks = remainingFireTicks
    }

    override fun getRemainingFireTicks(): Int {
        return delegate.remainingFireTicks
    }

    override fun knockback(power: Double, xd: Double, zd: Double, source: DamageSource, damage: Float, comesFromEffect: Boolean) {
        if (source.entity == delegate) return
        (delegate as? LivingEntity)?.knockback(power, xd, zd, source, damage, comesFromEffect)
    }

    override fun push(pushingEntity: Entity) {
        if (pushingEntity !== delegate) {
            delegate.push(pushingEntity)
        }
    }

    override fun canCollideWith(entity: Entity): Boolean {
        return checkCollide(entity) && delegate.canCollideWith(entity)
    }

    private fun checkCollide(entity: Entity): Boolean {
        return entity !== delegate &&
            passengers.none { it === entity } &&
            delegate.passengers.none { it === entity } &&
            (entity !is HitBoxEntityImpl || entity.delegate !== delegate)
    }

    override fun getActiveEffects(): Collection<MobEffectInstance> {
        return (delegate as? LivingEntity)?.activeEffects ?: emptyList()
    }

    override fun getControllingPassenger(): LivingEntity? {
        return if (!mounted) {
            null
        } else {
            interaction.firstPassenger as? LivingEntity ?: super.getControllingPassenger()
        }
    }

    override fun onWalk(): Boolean = isWalking

    private fun mountControl(player: ServerPlayer) {
        if (delegate !is LivingEntity ||
            !mountController.canFly() && delegate.isFallFlying
        ) {
            return
        }

        val travelVector = Vec3(
            delegate.xxa.toDouble(),
            delegate.yya.toDouble(),
            delegate.zza.toDouble()
        )
        updateFlyStatus(player)

        val riddenInput = rideInput(player, travelVector)
        if (riddenInput.length() > 0.01) {
            delegate.yRot = player.yRot
            if (onFly) {
                delegate.yHeadRot = player.yRot
            }

            val movementVector = Vec3(
                riddenInput.x.toDouble(),
                riddenInput.y.toDouble(),
                riddenInput.z.toDouble()
            )
            delegate.move(MoverType.SELF, movementVector)
        }

        if (!onFly &&
            mountController.canJump() &&
            (delegate.horizontalCollision || player.lastClientInput.jump()) &&
            (delegate.deltaMovement.y + delegate.gravity) in 0.0..0.01 && jumpDelay == 0
        ) {
            jumpDelay = 10
            delegate.jumpFromGround()
        }
    }

    private fun movementSpeed(): Float {
        if (delegate !is LivingEntity) {
            return 0.0f
        }

        val attribute = delegate.getAttribute(Attributes.MOVEMENT_SPEED) ?: return 0.0f
        val attributeValue = attribute.value.toFloat()

        if (onFly || shouldDiscardFriction()) {
            return attributeValue
        }

        return level()
            .getBlockState(blockPosBelowThatAffectsMyMovement)
            .block
            .getFriction() * attributeValue
    }

    private fun updateFlyStatus(player: ServerPlayer) {
        val fly = (player.lastClientInput.jump() && mountController.canFly()) ||
            noGravity ||
            onFly

        if (delegate is Mob) {
            delegate.isNoAi = fly
        } else {
            delegate.isNoGravity = fly
        }

        onFly = fly && !delegate.onGround()
        if (onFly) {
            delegate.resetFallDistance()
        }
    }

    private fun rideInput(player: ServerPlayer, travelVector: Vec3): Vector3f {
        return mountController.move(
            if (onFly) {
                MountController.MoveType.FLY
            } else {
                MountController.MoveType.DEFAULT
            },
            player.connection.wrap(),
            (delegate as LivingEntity).wrap(),
            Vector3f(
                player.xMovement(),
                player.yMovement(),
                player.zMovement()
            ),
            Vector3f(
                travelVector.x.toFloat(),
                travelVector.y.toFloat(),
                travelVector.z.toFloat()
            )
        )
            .mul(movementSpeed())
            .rotateY(-Math.toRadians(player.yRot.toDouble()).toFloat())
    }

    override fun tick() {
        delegate.removalReason?.let { removalReason ->
            if (!isRemoved) {
                remove(removalReason)
            }

            return
        }

        val controller = controllingPassenger
        if (jumpDelay > 0) {
            jumpDelay--
        }

        interaction.isInvisible = delegate.isInvisible
        if (controller is ServerPlayer && !isDeadOrDying && mountController.canControl()) {
            if (delegate is Mob) {
                delegate.navigation.stop()
            }

            mountControl(controller)
        } else {
            initialSetup()
        }

        yRot = bone.rotation().y
        yHeadRot = yRot
        yBodyRot = yRot

        val pos = relativePosition()
        val minusHeight = source.minY * bone.hitBoxScale()
        setPos(
            pos.x.toDouble(),
            pos.y.toDouble() + minusHeight,
            pos.z.toDouble()
        )

        BlockGetter.forEachBlockIntersectedBetween(
            oldPosition(),
            position(),
            boundingBox
        ) { pos, _ ->
            level().getBlockState(pos).entityInside(
                level(),
                pos,
                delegate,
                applier,
                true
            )
            true
        }
        applier.applyAndClear(delegate)

        if (isInLava) {
            delegate.lavaHurt()
        }

        firstTick = false
        listener.sync(this)
    }

    override fun remove(reason: RemovalReason) {
        initialSetup()

        listener.handle(HitBoxRemoveEvent(this))
        interaction.remove(reason)

        super.remove(reason)
    }

    override fun hasExactlyOnePlayerPassenger(): Boolean = false

    override fun isDeadOrDying(): Boolean = delegate is LivingEntity && delegate.isDeadOrDying

    override fun hide(player: PlatformPlayer) {
        TODO("with mixin")
    }

    override fun show(player: PlatformPlayer) {
        TODO("with mixin")
    }


    override fun interact(player: Player, hand: InteractionHand, vec: Vec3): InteractionResult {
        if (player === delegate) {
            return InteractionResult.FAIL
        }
        val serverPlayer = player as ServerPlayer

        val interact = HitBoxInteractAtEvent(
            serverPlayer.connection.wrap(),
            this,
            when (hand) {
                InteractionHand.MAIN_HAND -> ModelInteractionHand.RIGHT
                InteractionHand.OFF_HAND -> ModelInteractionHand.LEFT
            },
            vec.toVector3f()
        )
        if (!listener.handle(interact)) return InteractionResult.FAIL

        serverPlayer.connection.handleInteract(
            ServerboundInteractPacket(
                delegate.id,
                hand,
                vec,
                player.isShiftKeyDown
            )
        )
        return InteractionResult.SUCCESS
    }

    override fun addEffect(effectInstance: MobEffectInstance, entity: Entity?): Boolean {
        return if (entity == delegate) {
            false
        } else {
            delegate is LivingEntity &&
                delegate.addEffect(effectInstance, entity)
        }
    }

    override fun hurtServer(world: ServerLevel, source: DamageSource, amount: Float): Boolean {
        if (delegate == source.entity ||
            delegate.isInvulnerable ||
            source.entity == controllingPassenger && !mountController.canBeDamagedByRider()
        ) {
            return false
        }

        val sourceImpl = ModelDamageSourceImpl(source)
        val event = HitBoxDamagedEvent(this, sourceImpl, amount)
        if (!listener.handle(event)) return false

        return delegate is LivingEntity &&
            delegate.hurtServer(world, source, event.damage)
    }

    override fun deflection(projectile: Projectile): ProjectileDeflection {
        if (projectile.owner?.uuid == delegate.uuid) {
            return ProjectileDeflection.NONE
        }

        return (delegate as? LivingEntity)?.deflection(projectile)
            ?: ProjectileDeflection.NONE
    }

    override fun getHealth(): Float {
        return (delegate as? LivingEntity)?.health ?: super.getHealth()
    }

    override fun makeBoundingBox(vec3: Vec3): AABB {
        if (!initialized) {
            return super.makeBoundingBox(vec3)
        }

        val scale = bone.hitBoxScale()
        val boundingBox = AABB(
            vec3.x + source.minX * scale,
            vec3.y,
            vec3.z + source.minZ * scale,
            vec3.x + source.maxX * scale,
            vec3.y + source.y() * scale,
            vec3.z + source.maxZ * scale
        )

        if (CONFIG.debug().has(DebugConfig.DebugOption.HITBOX)) {
            val level = level() as ServerLevel
            val particleOptions = DustParticleOptions(Color.RED.rgb, 1F)

            level.sendParticles(
                particleOptions,
                true, true,
                boundingBox.minX, boundingBox.minY, boundingBox.minZ,
                1,
                0.0, 0.0, 0.0,
                1.0
            )
            level.sendParticles(
                particleOptions,
                true, true,
                boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ,
                1,
                0.0, 0.0, 0.0,
                1.0
            )
        }

        return boundingBox
    }

    override fun getDefaultDimensions(pose: Pose): EntityDimensions {
        return if (initialized) {
            calculateDimensions()
        } else {
            super.getDefaultDimensions(pose)
        }
    }

    override fun removeHitBox() {
        source().task {
            dismountAll()
            remove((delegate as? LivingEntity)?.removalReason ?: RemovalReason.KILLED)
        }
    }
}
