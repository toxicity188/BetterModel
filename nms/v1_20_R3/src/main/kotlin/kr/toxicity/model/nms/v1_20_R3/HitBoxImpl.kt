package kr.toxicity.model.nms.v1_20_R3

import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.bone.BoneName
import kr.toxicity.model.api.data.blueprint.ModelBoundingBox
import kr.toxicity.model.api.event.ModelDamagedEvent
import kr.toxicity.model.api.event.ModelInteractAtEvent
import kr.toxicity.model.api.event.ModelInteractEvent
import kr.toxicity.model.api.mount.MountController
import kr.toxicity.model.api.nms.HitBox
import kr.toxicity.model.api.nms.HitBoxListener
import kr.toxicity.model.api.nms.HitBoxSource
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
import net.minecraft.world.entity.ai.attributes.AttributeMap
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.craftbukkit.v1_20_R3.CraftServer
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftLivingEntity
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer
import org.bukkit.entity.Entity
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector
import org.joml.Quaterniond
import org.joml.Vector3f
import java.util.function.Supplier

internal class HitBoxImpl(
    private val name: BoneName,
    private val source: ModelBoundingBox,
    private val supplier: HitBoxSource,
    private val listener: HitBoxListener,
    private val delegate: LivingEntity,
    private var mountController: MountController
) : LivingEntity(EntityType.SILVERFISH, delegate.level()), HitBox {

    private var initialized = false
    private var jumpDelay = 0
    private var mounted = false
    private var collision = delegate.collides
    private var noGravity = if (delegate is Mob) delegate.isNoAi else delegate.isNoGravity
    private var forceDismount = false
    private var onFly = false

    val craftEntity: HitBox by lazy {
        object : CraftLivingEntity(Bukkit.getServer() as CraftServer, this), HitBox by this {}
    }
    private val _rotatedSource = FunctionUtil.throttleTick(Supplier {
        source.rotate(Quaterniond(supplier.hitBoxViewRotation()))
    })
    private val rotatedSource get() = _rotatedSource.get()
    val dimensions: EntityDimensions get() = rotatedSource.run {
        EntityDimensions(
            (x() + z()).toFloat() / 2,
            y().toFloat(),
            false
        ).scale(supplier.hitBoxScale())
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
        if (BetterModel.IS_PAPER) updatingSectionStatus = false
        refreshDimensions()
        level().addFreshEntity(this)
        level().addFreshEntity(interaction.apply {
            moveTo(delegate.position())
        })
        interaction.startRiding(this)
    }

    private fun initialSetup() {
        if (mounted) {
            mounted = false
            delegate.collides = collision
        }
    }

    override fun groupName(): BoneName = name
    override fun source(): Entity = delegate.bukkitEntity
    override fun forceDismount(): Boolean = forceDismount
    override fun mountController(): MountController = mountController
    override fun mountController(controller: MountController) {
        this.mountController = controller
    }
    override fun relativePosition(): Vector3f = delegate.position().run {
        supplier.hitBoxPosition().add(x.toFloat(), y.toFloat(), z.toFloat())
    }
    override fun listener(): HitBoxListener = listener

    override fun getArmorSlots(): MutableIterable<ItemStack> = mutableSetOf()
    override fun hasMountDriver(): Boolean = controllingPassenger != null
    override fun getItemBySlot(slot: EquipmentSlot): ItemStack = ItemStack.EMPTY
    override fun setItemSlot(slot: EquipmentSlot, stack: ItemStack) {
    }
    override fun getMainArm(): HumanoidArm = HumanoidArm.RIGHT
    
    override fun mount(entity: Entity) {
        if (controllingPassenger != null) return
        if (interaction.bukkitEntity.addPassenger(entity)) {
            if (mountController.canControl()) {
                mounted = true
                collision = delegate.collides
                noGravity = delegate.isNoGravity
                delegate.collides = false
            }
            listener.mount(craftEntity, entity)
        }
    }

    override fun dismount(entity: Entity) {
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

    override fun knockback(strength: Double, x: Double, z: Double) {
        delegate.knockback(strength, x, z)
    }

    override fun push(pushingEntity: net.minecraft.world.entity.Entity) {
        if (pushingEntity === delegate) return
        delegate.push(pushingEntity)
    }

    override fun push(x: Double, y: Double, z: Double, pushingEntity: net.minecraft.world.entity.Entity?) {
        if (pushingEntity === delegate) return
        delegate.push(x, y, z, pushingEntity)
    }

    override fun isCollidable(ignoreClimbing: Boolean): Boolean {
        return delegate.isCollidable(ignoreClimbing)
    }

    override fun canCollideWith(entity: net.minecraft.world.entity.Entity): Boolean {
        return checkCollide(entity) && delegate.canCollideWith(entity)
    }

    override fun canCollideWithBukkit(entity: net.minecraft.world.entity.Entity): Boolean {
        return checkCollide(entity) && delegate.canCollideWithBukkit(entity)
    }

    private fun checkCollide(entity: net.minecraft.world.entity.Entity): Boolean {
        return entity !== delegate
                && passengers.none { it === entity }
                && delegate.passengers.none { it === entity }
                && (entity !is HitBoxImpl || entity.delegate !== delegate)
    }

    override fun getActiveEffects(): Collection<MobEffectInstance?> {
        return delegate.getActiveEffects()
    }

    override fun getControllingPassenger(): LivingEntity? {
        return if (mounted) interaction.firstPassenger as? LivingEntity ?: super.getControllingPassenger() else null
    }

    override fun onWalk(): Boolean {
        return isWalking()
    }

    private fun mountControl(player: ServerPlayer, travelVector: Vec3) {
        if (!mountController.canFly() && delegate.isFallFlying) return

        updateFlyStatus(player)
        val riddenInput = rideInput(player, travelVector)
        val movement = riddenInput
            .mul(movementSpeed())
            .rotateY(-Math.toRadians(player.yRot.toDouble()).toFloat())

        if (movement.length() > 0.01) {
            delegate.yBodyRot = player.yRot
            if (onFly) delegate.yHeadRot = player.yRot
            delegate.move(MoverType.SELF, Vec3(riddenInput.x.toDouble(), riddenInput.y.toDouble(), riddenInput.z.toDouble()))
        }
        val dy = delegate.deltaMovement.y + delegate.gravity
        if (!onFly && mountController.canJump() && (delegate.horizontalCollision || player.isJump()) && dy in 0.0..0.01 && jumpDelay == 0) {
            jumpDelay = 10
            delegate.jumpFromGround()
        }
    }

    private fun movementSpeed() = delegate.getAttribute(Attributes.MOVEMENT_SPEED)?.let {
        it.value.toFloat() * (if (!onFly && !delegate.shouldDiscardFriction()) delegate.level()
            .getBlockState(blockPosBelowThatAffectsMyMovement)
            .block
            .getFriction() else 1.0F)
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
            mountControl(controller, Vec3(delegate.xxa.toDouble(), delegate.yya.toDouble(), delegate.zza.toDouble()))
        } else initialSetup()
        yRot = supplier.hitBoxRotation().y
        yHeadRot = yRot
        yBodyRot = yRot
        val pos = relativePosition()
        val minusHeight = rotatedSource.minY * supplier.hitBoxScale() - type.height
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
        return delegate.isDeadOrDying
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
        val plugin = BetterModel.plugin() as Plugin
        player.hideEntity(plugin, bukkitEntity)
        player.hideEntity(plugin, interaction.bukkitEntity)
    }

    override fun show(player: org.bukkit.entity.Player) {
        val plugin = BetterModel.plugin() as Plugin
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
        return delegate.addEffect(effectInstance, cause)
    }

    override fun addEffect(effectInstance: MobEffectInstance, entity: net.minecraft.world.entity.Entity?): Boolean {
        if (entity === delegate) return false
        return delegate.addEffect(effectInstance, entity)
    }

    override fun addEffect(
        effectInstance: MobEffectInstance,
        entity: net.minecraft.world.entity.Entity?,
        cause: EntityPotionEffectEvent.Cause
    ): Boolean {
        if (entity === delegate) return false
        return delegate.addEffect(effectInstance, entity, cause)
    }

    override fun hurt(source: DamageSource, amount: Float): Boolean {
        if (source.entity === delegate || delegate.isInvulnerable) return false
        if (source.entity === controllingPassenger && !mountController.canBeDamagedByRider()) return false
        val ds = ModelDamageSourceImpl(source)
        val event = ModelDamagedEvent(craftEntity, ds, amount)
        if (!event.call()) return false
        if (listener.damage(this, ds, amount.toDouble())) return false
        return delegate.hurt(source, event.damage)
    }

    override fun getHealth(): Float {
        return delegate.health
    }

    override fun getAttributes(): AttributeMap {
        return if (initialized) delegate.attributes else super.getAttributes()
    }

    override fun makeBoundingBox(): AABB {
        return if (!initialized) {
            super.makeBoundingBox()
        } else {
            val vec3 = position()
            val scale = supplier.hitBoxScale()
            val source = rotatedSource
            AABB(
                vec3.x + source.minX * scale,
                vec3.y + type.height,
                vec3.z + source.minZ * scale,
                vec3.x + source.maxX * scale,
                vec3.y + (source.maxY - source.minY) * scale + type.height,
                vec3.z + source.maxZ * scale
            ).apply {
                if (CONFIG.debug().hitBox) {
                    bukkitEntity.world.spawnParticle(Particle.REDSTONE, minX, minY, minZ, 1, 0.0, 0.0, 0.0, 0.0, Particle.DustOptions(Color.RED, 1F))
                    bukkitEntity.world.spawnParticle(Particle.REDSTONE, maxX, maxY, maxZ, 1, 0.0, 0.0, 0.0, 0.0, Particle.DustOptions(Color.RED, 1F))
                }
            }
        }
    }

    override fun getDimensions(pose: Pose): EntityDimensions = dimensions

    override fun removeHitBox() {
        BetterModel.plugin().scheduler().task(bukkitEntity) {
            remove(delegate.removalReason ?: RemovalReason.KILLED)
        }
    }
}