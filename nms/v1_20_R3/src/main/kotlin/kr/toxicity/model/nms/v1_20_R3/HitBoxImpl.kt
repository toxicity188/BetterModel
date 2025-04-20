package kr.toxicity.model.nms.v1_20_R3

import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.bone.BoneName
import kr.toxicity.model.api.data.blueprint.ModelBoundingBox
import kr.toxicity.model.api.event.ModelDamagedEvent
import kr.toxicity.model.api.event.ModelInteractEvent
import kr.toxicity.model.api.event.ModelInteractEvent.Hand
import kr.toxicity.model.api.mount.MountController
import kr.toxicity.model.api.nms.EntityAdapter
import kr.toxicity.model.api.nms.HitBox
import kr.toxicity.model.api.nms.HitBoxListener
import kr.toxicity.model.api.nms.HitBoxSource
import net.minecraft.core.BlockPos
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
import net.minecraft.world.item.Items
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.v1_20_R3.CraftServer
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftLivingEntity
import org.bukkit.entity.Entity
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.joml.Vector3f
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

class HitBoxImpl(
    private val name: BoneName,
    private val source: ModelBoundingBox,
    private val supplier: HitBoxSource,
    private val listener: HitBoxListener,
    private val delegate: LivingEntity,
    private var mountController: MountController,
    private val adapter: EntityAdapter
) : LivingEntity(EntityType.SLIME, delegate.level()), HitBox {

    private var initialized = false
    private var jumpDelay = 0
    private var mounted = false
    private var collision = delegate.collides
    private var noGravity = if (delegate is Mob) delegate.isNoAi else delegate.isNoGravity
    private var forceDismount = false
    private var onFly = false

    init {
        moveTo(delegate.position())
        if (!BetterModel.inst().configManager().debug().hitBox()) isInvisible = true
        persist = false
        isSilent = true
        initialized = true
        if (BetterModel.IS_PAPER) updatingSectionStatus = false
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
    override fun relativePosition(): Vector3f = position().run {
        Vector3f(x.toFloat(), y.toFloat(), z.toFloat())
    }
    override fun listener(): HitBoxListener = listener

    val craftEntity: HitBox by lazy {
        object : CraftLivingEntity(Bukkit.getServer() as CraftServer, this), HitBox by this {}
    }
    override fun getArmorSlots(): MutableIterable<ItemStack> = mutableSetOf()
    override fun hasMountDriver(): Boolean = controllingPassenger != null
    override fun getItemBySlot(slot: EquipmentSlot): ItemStack = Items.AIR.defaultInstance
    override fun setItemSlot(slot: EquipmentSlot, stack: ItemStack) {
    }
    override fun getMainArm(): HumanoidArm = HumanoidArm.RIGHT
    
    override fun mount(entity: Entity) {
        if (!mountController.canMount()) return
        if (firstPassenger != null) return
        if (bukkitEntity.addPassenger(entity)) {
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
        if (bukkitEntity.removePassenger(entity)) listener.dismount(craftEntity, entity)
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
        return if (mounted) firstPassenger as? LivingEntity ?: super.getControllingPassenger() else null
    }

    override fun onWalk(): Boolean {
        return isWalking()
    }

    private fun mountControl(player: ServerPlayer, travelVector: Vec3) {
        if (!mountController.canFly() && delegate.isFallFlying) return
        val fly = (player.isJump() && mountController.canFly()) || noGravity || onFly
        if (delegate is Mob) delegate.isNoAi = fly
        else delegate.isNoGravity = fly
        onFly = fly && !delegate.onGround()
        if (onFly) delegate.resetFallDistance()
        val riddenInput = mountController.move(
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
        )
        val f = if (!onFly && !delegate.shouldDiscardFriction()) delegate.level()
            .getBlockState(blockPosBelowThatAffectsMyMovement)
            .block
            .getFriction() else 1.0f
        val speed = delegate.getAttributeValue(Attributes.MOVEMENT_SPEED).toFloat() * f
        val movement = riddenInput
            .mul(speed)
            .rotateY(-Math.toRadians(player.yRot.toDouble()).toFloat())
        if (movement.length() > 0.01) {
            delegate.yBodyRot = player.yRot
            if (onFly) delegate.yHeadRot = player.yRot
            delegate.move(MoverType.SELF, Vec3(riddenInput.x.toDouble(), riddenInput.y.toDouble(), riddenInput.z.toDouble()))
        }
        val dy = delegate.deltaMovement.y + delegate.gravity
        if (!onFly && mountController.canJump() && (delegate.horizontalCollision || player.isJump()) && dy >= 0.0 && dy <= 0.01 && jumpDelay == 0) {
            jumpDelay = 10
            delegate.jumpFromGround()
        }
    }

    private val boxHeight
        get() = source.lengthZX() / 2 * adapter.scale()
    
    override fun tick() {
        if (!delegate.valid) {
            if (valid) remove(delegate.removalReason ?: RemovalReason.KILLED)
            return
        }
        entityData.set(SLIME_SIZE, ceil(boxHeight / 0.52).roundToInt())
        val controller = controllingPassenger
        if (jumpDelay > 0) jumpDelay--
        health = delegate.health
        if (controller is ServerPlayer && !isDeadOrDying && mountController.canControl()) {
            if (delegate is Mob) delegate.navigation.stop()
            mountControl(controller, Vec3(delegate.xxa.toDouble(), delegate.yya.toDouble(), delegate.zza.toDouble()))
        } else initialSetup()
        val rotation = supplier.hitBoxRotation()
        yRot = rotation.y
        yHeadRot = yRot
        yBodyRot = yRot
        val transform = supplier.hitBoxPosition()
        val pos = delegate.position()
        setPos(
            pos.x + transform.x,
            pos.y + transform.y + source.maxY * adapter.scale() - boxHeight,
            pos.z + transform.z
        )
        BlockPos.betweenClosedStream(boundingBox).forEach {
            level().getBlockState(it).entityInside(level(), it, delegate)
        }
        updateInWaterStateAndDoFluidPushing()
        if (isInLava) delegate.lavaHurt()
        setSharedFlagOnFire(delegate.remainingFireTicks > 0)
        firstTick = false
        listener.sync(craftEntity)
    }

    override fun remove(reason: RemovalReason) {
        initialSetup()
        listener.remove(craftEntity)
        super.remove(reason)
    }

    override fun getBukkitLivingEntity(): CraftLivingEntity = bukkitEntity
    override fun getBukkitEntity(): CraftLivingEntity = craftEntity as CraftLivingEntity

    private val dimensions = EntityDimensions(
        max(source.x(), source.z()).toFloat(),
        source.y().toFloat(),
        true
    )

    override fun isDeadOrDying(): Boolean {
        return delegate.isDeadOrDying
    }

    override fun getDismountLocationForPassenger(passenger: LivingEntity): Vec3 {
        return delegate.getDismountLocationForPassenger(passenger)
    }

    override fun interact(player: Player, hand: InteractionHand): InteractionResult {
        val interact = ModelInteractEvent(player.bukkitEntity as org.bukkit.entity.Player, craftEntity, when (hand) {
            MAIN_HAND -> Hand.RIGHT
            OFF_HAND -> Hand.LEFT
        })
        if (!interact.call()) return InteractionResult.FAIL
        return delegate.interact(player, hand)
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
        if (source.entity === delegate) return false
        val ds = ModelDamageSourceImpl(source)
        val event = ModelDamagedEvent(craftEntity, ds, amount)
        if (!event.call()) return false
        if (listener.damage(ds, amount.toDouble())) return false
        return delegate.hurt(source, event.damage)
    }

    override fun getAttributes(): AttributeMap {
        val attr = super.getAttributes()
        if (initialized) {
            delegate.getAttribute(Attributes.MAX_HEALTH)?.let {
                attr.getInstance(Attributes.MAX_HEALTH)?.baseValue = it.baseValue
            }
        }
        return attr
    }

    override fun makeBoundingBox(): AABB {
        return if (!initialized) {
            super.makeBoundingBox()
        } else {
            val pos = position()
            val scale = adapter.scale()
            AABB(
                pos.x - source.minX * scale,
                pos.y + source.minY * scale,
                pos.z - source.minZ * scale,
                pos.x - source.maxX * scale,
                pos.y + source.maxY * scale,
                pos.z - source.maxZ * scale
            )
        }
    }

    override fun getDimensions(pose: Pose): EntityDimensions = dimensions

    override fun removeHitBox() {
        BetterModel.inst().scheduler().task(bukkitEntity) {
            remove(delegate.removalReason ?: RemovalReason.KILLED)
        }
    }
}