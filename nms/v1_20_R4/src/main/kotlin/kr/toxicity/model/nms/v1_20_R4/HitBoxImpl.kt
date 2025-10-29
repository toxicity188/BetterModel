/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.nms.v1_20_R4

import io.papermc.paper.event.entity.EntityKnockbackEvent
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.bone.BoneName
import kr.toxicity.model.api.bone.RenderedBone
import kr.toxicity.model.api.config.DebugConfig
import kr.toxicity.model.api.data.blueprint.ModelBoundingBox
import kr.toxicity.model.api.event.ModelDamagedEvent
import kr.toxicity.model.api.event.ModelInteractAtEvent
import kr.toxicity.model.api.event.ModelInteractEvent
import kr.toxicity.model.api.mount.MountController
import kr.toxicity.model.api.nms.HitBox
import kr.toxicity.model.api.nms.HitBoxListener
import kr.toxicity.model.api.nms.ModelInteractionHand
import kr.toxicity.model.api.util.FunctionUtil
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionHand.MAIN_HAND
import net.minecraft.world.InteractionHand.OFF_HAND
import net.minecraft.world.InteractionResult
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.entity.projectile.ProjectileDeflection
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.craftbukkit.CraftServer
import org.bukkit.craftbukkit.entity.CraftArmorStand
import org.bukkit.craftbukkit.entity.CraftLivingEntity
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.util.Vector
import org.joml.Quaterniond
import org.joml.Vector3f
import java.util.*
import java.util.function.Supplier

internal class HitBoxImpl(
    private val name: BoneName,
    private val source: ModelBoundingBox,
    private val bone: RenderedBone,
    private val listener: HitBoxListener,
    private val delegate: Entity,
    private var mountController: MountController
) : ArmorStand(EntityType.ARMOR_STAND, delegate.level()), HitBox {
    private var initialized = false
    private var jumpDelay = 0
    private var mounted = false
    private var collision = ifLivingEntity { collides } == true
    private var noGravity = if (delegate is Mob) delegate.isNoAi else delegate.isNoGravity
    private var forceDismount = false
    private var onFly = false

    val craftEntity: HitBox by lazy {
        object : CraftArmorStand(Bukkit.getServer() as CraftServer, this), HitBox by this {}
    }
    private val _rotatedSource = FunctionUtil.throttleTick(Supplier {
        source.rotate(Quaterniond(bone.hitBoxViewRotation()))
    })
    private val rotatedSource get() = _rotatedSource.get()
    val dimensions: EntityDimensions get() = rotatedSource.run {
        EntityDimensions(
            (x() + z()).toFloat() / 2,
            y().toFloat(),
            delegate.eyeHeight,
            EntityAttachments.createDefault(0F, 0F),
            false
        ).scale(bone.hitBoxScale())
    }
    private val interaction by lazy {
        HitBoxInteraction(this)
    }

    init {
        moveTo(delegate.position())
        isInvisible = true
        persist = false
        isSilent = true
        initialized = true
        level().addFreshEntity(this, CreatureSpawnEvent.SpawnReason.CUSTOM)
        level().addFreshEntity(interaction.apply {
            moveTo(delegate.position())
        }, CreatureSpawnEvent.SpawnReason.CUSTOM)
        interaction.startRiding(this)
    }

    private fun initialSetup() {
        if (mounted) {
            mounted = false
            if (delegate is Mob) delegate.isNoAi = noGravity
            else delegate.isNoGravity = noGravity
            ifLivingEntity { collides = collision }
        }
    }

    override fun groupName(): BoneName = name
    override fun id(): Int = id
    override fun uuid(): UUID = uuid
    override fun source(): org.bukkit.entity.Entity = delegate.bukkitEntity
    override fun positionSource(): RenderedBone = bone
    override fun forceDismount(): Boolean = forceDismount
    override fun mountController(): MountController = mountController
    override fun mountController(controller: MountController) {
        this.mountController = controller
    }
    override fun relativePosition(): Vector3f = delegate.position().run {
        bone.hitBoxPosition().add(x.toFloat(), y.toFloat(), z.toFloat())
    }
    override fun listener(): HitBoxListener = listener

    override fun getArmorSlots(): MutableIterable<ItemStack> = mutableSetOf()
    override fun hasMountDriver(): Boolean = controllingPassenger != null
    override fun getItemBySlot(slot: EquipmentSlot): ItemStack = ItemStack.EMPTY
    override fun setItemSlot(slot: EquipmentSlot, stack: ItemStack) {
    }
    override fun getMainArm(): HumanoidArm = HumanoidArm.RIGHT
    
    override fun mount(entity: org.bukkit.entity.Entity) {
        if (controllingPassenger != null) return
        if (interaction.bukkitEntity.addPassenger(entity)) {
            if (mountController.canControl()) {
                mounted = true
                noGravity = delegate.isNoGravity
                ifLivingEntity {
                    collision = collides
                    collides = false
                }
            }
            listener.mount(craftEntity, entity)
        }
    }

    override fun dismount(entity: org.bukkit.entity.Entity) {
        forceDismount = true
        if (interaction.bukkitEntity.removePassenger(entity)) listener.dismount(craftEntity, entity)
        forceDismount = false
    }

    override fun dismountAll() {
        forceDismount = true
        interaction.passengers.forEach {
            it.stopRiding(true)
            listener.dismount(craftEntity, it.bukkitEntity)
        }
        forceDismount = false
    }

    override fun setRemainingFireTicks(remainingFireTicks: Int) {
        delegate.remainingFireTicks = remainingFireTicks
    }

    override fun getRemainingFireTicks(): Int {
        return delegate.remainingFireTicks
    }

    override fun knockback(
        d0: Double,
        d1: Double,
        d2: Double,
        attacker: Entity?,
        cause: EntityKnockbackEvent.Cause
    ) {
        if (attacker === delegate) return
        ifLivingEntity { knockback(d0, d1, d2, attacker, cause) }
    }

    override fun push(pushingEntity: Entity) {
        if (pushingEntity === delegate) return
        delegate.push(pushingEntity)
    }

    override fun push(x: Double, y: Double, z: Double, pushingEntity: Entity?) {
        if (pushingEntity === delegate) return
        delegate.push(x, y, z, pushingEntity)
    }

    override fun isCollidable(ignoreClimbing: Boolean): Boolean {
        return delegate.isCollidable(ignoreClimbing)
    }

    override fun canCollideWith(entity: Entity): Boolean {
        return checkCollide(entity) && delegate.canCollideWith(entity)
    }

    override fun canCollideWithBukkit(entity: Entity): Boolean {
        return checkCollide(entity) && delegate.canCollideWithBukkit(entity)
    }

    private fun checkCollide(entity: Entity): Boolean {
        return entity !== delegate
                && passengers.none { it === entity }
                && delegate.passengers.none { it === entity }
                && (entity !is HitBoxImpl || entity.delegate !== delegate)
    }

    override fun getActiveEffects(): Collection<MobEffectInstance?> {
        return ifLivingEntity { getActiveEffects() } ?: emptyList()
    }

    override fun getControllingPassenger(): LivingEntity? {
        return if (mounted) interaction.firstPassenger as? LivingEntity ?: super.getControllingPassenger() else null
    }

    override fun onWalk(): Boolean {
        return isWalking()
    }

    private fun mountControl(player: ServerPlayer) {
        if (delegate !is LivingEntity) return
        val travelVector = Vec3(delegate.xxa.toDouble(), delegate.yya.toDouble(), delegate.zza.toDouble())
        if (!mountController.canFly() && delegate.isFallFlying) return
        
        updateFlyStatus(player)
        val riddenInput = rideInput(player, travelVector)
        if (riddenInput.length() > 0.01) {
            delegate.yRot = player.yRot
            if (onFly) delegate.yHeadRot = player.yRot
            delegate.move(MoverType.SELF, Vec3(riddenInput.x.toDouble(), riddenInput.y.toDouble(), riddenInput.z.toDouble()))
        }
        val dy = delegate.deltaMovement.y + delegate.gravity
        if (!onFly && mountController.canJump() && (delegate.horizontalCollision || player.isJump()) && dy in 0.0..0.01 && jumpDelay == 0) {
            jumpDelay = 10
            delegate.jumpFromGround()
        }
    }
    
    private fun movementSpeed() = ifLivingEntity {
        getAttribute(Attributes.MOVEMENT_SPEED)?.value?.toFloat()?.let {
            if (!onFly && !shouldDiscardFriction()) level()
                .getBlockState(blockPosBelowThatAffectsMyMovement)
                .block
                .getFriction() * it else it
        } ?: 0.0F
    } ?: 0.0F

    private fun updateFlyStatus(player: ServerPlayer) {
        val fly = (player.isJump() && mountController.canFly()) || noGravity || onFly
        if (delegate is Mob) delegate.isNoAi = fly
        else delegate.isNoGravity = fly
        onFly = fly && !delegate.onGround()
        if (onFly) delegate.resetFallDistance()
    }

    private fun rideInput(player: ServerPlayer, travelVector: Vec3) = mountController.move(
        if (onFly) MountController.MoveType.FLY else MountController.MoveType.DEFAULT,
        player.bukkitEntity,
        delegate.bukkitEntity as org.bukkit.entity.LivingEntity,
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
    ).mul(movementSpeed()).rotateY(-Math.toRadians(player.yRot.toDouble()).toFloat())

    override fun tick() {
        delegate.removalReason?.let {
            if (!isRemoved) remove(it)
            return
        }
        val controller = controllingPassenger
        if (jumpDelay > 0) jumpDelay--
        interaction.isInvisible = delegate.isInvisible
        if (controller is ServerPlayer && !isDeadOrDying && mountController.canControl()) {
            if (delegate is Mob) delegate.navigation.stop()
            mountControl(controller)
        } else initialSetup()
        yRot = bone.rotation().y
        yHeadRot = yRot
        yBodyRot = yRot
        val pos = relativePosition()
        val minusHeight = rotatedSource.minY * bone.hitBoxScale()
        setPos(
            pos.x.toDouble(),
            pos.y.toDouble() + minusHeight,
            pos.z.toDouble()
        )
        BlockPos.betweenClosedStream(boundingBox).forEach {
            level().getBlockState(it).entityInside(level(), it, delegate)
        }
        updateInWaterStateAndDoFluidPushing()
        if (isInLava) delegate.lavaHurt()
        firstTick = false
        listener.sync(craftEntity)
    }

    @Suppress("removal", "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "WRONG_NULLABILITY_FOR_JAVA_OVERRIDE")
    override fun remove(reason: RemovalReason, cause: org.bukkit.event.entity.EntityRemoveEvent.Cause?) { //Compiler incorrectly considers it as non-null by some reason :(
        initialSetup()
        listener.remove(craftEntity)
        interaction.remove(reason)
        super.remove(reason, cause)
    }

    override fun getBukkitLivingEntity(): CraftLivingEntity = bukkitEntity
    override fun getBukkitEntity(): CraftLivingEntity = craftEntity as CraftLivingEntity
    override fun getBukkitEntityRaw(): CraftLivingEntity = bukkitEntity
    override fun hasExactlyOnePlayerPassenger(): Boolean = false

    override fun isDeadOrDying(): Boolean {
        return ifLivingEntity { isDeadOrDying } == true
    }

    override fun triggerInteract(player: org.bukkit.entity.Player, hand: ModelInteractionHand) {
        interact(
            (player as CraftPlayer).handle,
            when (hand) {
                ModelInteractionHand.LEFT -> OFF_HAND
                ModelInteractionHand.RIGHT -> MAIN_HAND
            }
        )
    }

    override fun triggerInteractAt(player: org.bukkit.entity.Player, hand: ModelInteractionHand, position: Vector) {
        interactAt(
            (player as CraftPlayer).handle,
            position.toVanilla(),
            when (hand) {
                ModelInteractionHand.LEFT -> OFF_HAND
                ModelInteractionHand.RIGHT -> MAIN_HAND
            }
        )
    }

    override fun hide(player: org.bukkit.entity.Player) {
        val plugin = BetterModel.plugin()
        player.hideEntity(plugin, bukkitEntity)
        player.hideEntity(plugin, interaction.bukkitEntity)
    }

    override fun show(player: org.bukkit.entity.Player) {
        val plugin = BetterModel.plugin()
        player.showEntity(plugin, bukkitEntity)
        player.showEntity(plugin, interaction.bukkitEntity)
    }

    override fun interact(player: Player, hand: InteractionHand): InteractionResult {
        if (player === delegate) return InteractionResult.FAIL
        val interact = ModelInteractEvent(player.bukkitEntity as org.bukkit.entity.Player, craftEntity, when (hand) {
            MAIN_HAND -> ModelInteractionHand.RIGHT
            OFF_HAND -> ModelInteractionHand.LEFT
        })
        if (!interact.call()) return InteractionResult.FAIL
        (player as ServerPlayer).connection.handleInteract(ServerboundInteractPacket.createInteractionPacket(delegate, player.isShiftKeyDown, hand))
        return InteractionResult.SUCCESS
    }

    override fun interactAt(player: Player, vec: Vec3, hand: InteractionHand): InteractionResult {
        if (player === delegate) return InteractionResult.FAIL
        val interact = ModelInteractAtEvent(player.bukkitEntity as org.bukkit.entity.Player, craftEntity, when (hand) {
            MAIN_HAND -> ModelInteractionHand.RIGHT
            OFF_HAND -> ModelInteractionHand.LEFT
        }, vec.toBukkit())
        if (!interact.call()) return InteractionResult.FAIL
        (player as ServerPlayer).connection.handleInteract(ServerboundInteractPacket.createInteractionPacket(delegate, player.isShiftKeyDown, hand, vec))
        return InteractionResult.SUCCESS
    }

    override fun addEffect(effectInstance: MobEffectInstance, cause: EntityPotionEffectEvent.Cause): Boolean {
        return ifLivingEntity { addEffect(effectInstance, cause) } == true
    }

    override fun addEffect(effectInstance: MobEffectInstance, entity: Entity?): Boolean {
        if (entity === delegate) return false
        return ifLivingEntity { addEffect(effectInstance, entity) } == true
    }

    override fun addEffect(
        effectInstance: MobEffectInstance,
        entity: Entity?,
        cause: EntityPotionEffectEvent.Cause
    ): Boolean {
        if (entity === delegate) return false
        return ifLivingEntity { addEffect(effectInstance, entity, cause) } == true
    }

    override fun hurt(source: DamageSource, amount: Float): Boolean {
        if (source.entity === delegate || delegate.isInvulnerable) return false
        if (source.entity === controllingPassenger && !mountController.canBeDamagedByRider()) return false
        val ds = ModelDamageSourceImpl(source)
        val event = ModelDamagedEvent(craftEntity, ds, amount)
        if (!event.call()) return false
        if (listener.damage(this, ds, amount.toDouble())) return false
        return ifLivingEntity { hurt(source, event.damage) } == true
    }

    override fun deflection(projectile: Projectile): ProjectileDeflection {
        if (projectile.owner === delegate) return ProjectileDeflection.NONE
        return ifLivingEntity { deflection(projectile) } ?: ProjectileDeflection.NONE
    }

    override fun getHealth(): Float {
        return ifLivingEntity { health } ?: super.getHealth()
    }

    override fun makeBoundingBox(): AABB {
        return if (!initialized) {
            super.makeBoundingBox()
        } else {
            val vec3 = position()
            val scale = bone.hitBoxScale()
            val source = rotatedSource
            AABB(
                vec3.x + source.minX * scale,
                vec3.y,
                vec3.z + source.minZ * scale,
                vec3.x + source.maxX * scale,
                vec3.y + source.y() * scale,
                vec3.z + source.maxZ * scale
            ).apply {
                if (CONFIG.debug().has(DebugConfig.DebugOption.HITBOX)) {
                    bukkitEntity.world.spawnParticle(Particle.DUST, minX, minY, minZ, 1, 0.0, 0.0, 0.0, 0.0, Particle.DustOptions(Color.RED, 1F))
                    bukkitEntity.world.spawnParticle(Particle.DUST, maxX, maxY, maxZ, 1, 0.0, 0.0, 0.0, 0.0, Particle.DustOptions(Color.RED, 1F))
                }
            }
        }
    }

    override fun getDefaultDimensions(pose: Pose): EntityDimensions = if (initialized) dimensions else super.getDefaultDimensions(pose)

    override fun removeHitBox() {
        BetterModel.plugin().scheduler().task(bukkitEntity) {
            dismountAll()
            remove(ifLivingEntity { removalReason } ?: RemovalReason.KILLED)
        }
    }

    private inline fun <T> ifLivingEntity(block: LivingEntity.() -> T): T? {
        return if (delegate.valid) (delegate as? LivingEntity)?.block() else null
    }
}