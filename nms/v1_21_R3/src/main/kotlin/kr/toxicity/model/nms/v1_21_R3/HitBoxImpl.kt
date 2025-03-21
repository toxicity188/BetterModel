package kr.toxicity.model.nms.v1_21_R3

import io.papermc.paper.event.entity.EntityKnockbackEvent
import kr.toxicity.model.api.BetterModel
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
import net.minecraft.server.level.ServerLevel
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
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.entity.projectile.ProjectileDeflection
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.CraftServer
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.craftbukkit.entity.CraftLivingEntity
import org.bukkit.entity.Entity
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.joml.Vector3f
import kotlin.math.max

class HitBoxImpl(
    private val name: String,
    private val boxHeight: Double,
    private val source: ModelBoundingBox,
    private val supplier: HitBoxSource,
    private val listener: HitBoxListener,
    private val delegate: LivingEntity,
    private val mountController: MountController,
    private val adapter: EntityAdapter
) : LivingEntity(EntityType.SLIME, delegate.level()), HitBox {
    private var initialized = false
    private var jumpDelay = 0

    init {
        moveTo(delegate.position())
        if (!BetterModel.inst().configManager().debug().hitBox()) isInvisible = true
        persist = false
        isSilent = true
        initialized = true
        if (BetterModel.IS_PAPER) `moonrise$setUpdatingSectionStatus`(false)
    }

    override fun name(): String = name
    override fun source(): Entity = delegate.bukkitEntity
    override fun mountController(): MountController = mountController
    override fun relativePosition(): Vector3f = position().run {
        Vector3f(x.toFloat(), y.toFloat(), z.toFloat())
    }
    override fun listener(): HitBoxListener = listener

    private var craftEntity: CraftLivingEntity? = null
    override fun getArmorSlots(): MutableIterable<ItemStack> = mutableSetOf()
    override fun getItemBySlot(slot: EquipmentSlot): ItemStack = Items.AIR.defaultInstance
    override fun setItemSlot(slot: EquipmentSlot, stack: ItemStack) {
    }
    override fun getMainArm(): HumanoidArm = HumanoidArm.RIGHT

    override fun addPassenger(entity: Entity) {
        if (!mountController.canMount()) return
        if (controllingPassenger != null) return
        bukkitEntity.addPassenger(entity)
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
        attacker: net.minecraft.world.entity.Entity?,
        cause: EntityKnockbackEvent.Cause
    ) {
        delegate.knockback(d0, d1, d2, attacker, cause)
    }

    override fun push(x: Double, y: Double, z: Double, pushingEntity: net.minecraft.world.entity.Entity?) {
        delegate.push(x, y, z, pushingEntity)
    }

    override fun canCollideWith(entity: net.minecraft.world.entity.Entity): Boolean {
        return entity !== delegate && (entity !is HitBoxImpl || entity.delegate !== delegate) && delegate.canCollideWith(entity)
    }

    override fun canCollideWithBukkit(entity: net.minecraft.world.entity.Entity): Boolean {
        return entity !== delegate && (entity !is HitBoxImpl || entity.delegate !== delegate) && delegate.canCollideWithBukkit(entity)
    }

    override fun getActiveEffects(): Collection<MobEffectInstance?> {
        return delegate.getActiveEffects()
    }

    override fun getControllingPassenger(): LivingEntity? {
        return firstPassenger as? LivingEntity ?: super.getControllingPassenger()
    }

    override fun onWalk(): Boolean {
        return isWalking()
    }

    private fun mountControl(player: ServerPlayer, travelVector: Vec3) {
        if (!mountController.canFly() && delegate.isFallFlying) return
        val riddenInput = mountController.move(
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
        val speed = delegate.getAttributeValue(Attributes.MOVEMENT_SPEED).toFloat()
        val movement = riddenInput
            .mul(speed)
            .rotateY(-Math.toRadians(player.yRot.toDouble()).toFloat())
        val dy = delegate.deltaMovement.y + delegate.gravity
        if (movement.length() > 0.01) {
            delegate.yBodyRot = player.yRot
            delegate.move(MoverType.SELF, Vec3(riddenInput.x.toDouble(), riddenInput.y.toDouble(), riddenInput.z.toDouble()))
        }
        if (mountController.canJump() && (delegate.horizontalCollision || player.isJump()) && dy >= 0.0 && dy <= 0.01 && jumpDelay == 0) {
            jumpDelay = 10
            delegate.jumpFromGround()
        }
    }

    override fun tick() {
        val controller = controllingPassenger
        if (jumpDelay > 0) jumpDelay--
        health = delegate.health
        if (controller is ServerPlayer && !isDeadOrDying && mountController.canControl()) {
            if (delegate is Mob) delegate.navigation.stop()
            mountControl(controller, Vec3(delegate.xxa.toDouble(), delegate.yya.toDouble(), delegate.zza.toDouble()))
        }
        yRot = supplier.hitBoxRotation().y
        yHeadRot = yRot
        yBodyRot = yRot
        val transform = supplier.hitBoxPosition()
        setPos(delegate.position().add(
            transform.x.toDouble(),
            transform.y.toDouble() + delegate.passengerPosition(adapter.scale()).y + source.maxY - boxHeight,
            transform.z.toDouble()
        ))
        BlockPos.betweenClosedStream(boundingBox).forEach {
            level().getBlockState(it).entityInside(level(), it, delegate)
        }
        updateInWaterStateAndDoFluidPushing()
        if (isInLava) delegate.lavaHurt()
        setSharedFlagOnFire(delegate.remainingFireTicks > 0)
        firstTick = false
        listener.sync(this)
    }

    override fun remove(reason: RemovalReason) {
        BetterModel.inst().scheduler().task(bukkitEntity.location) {
            super.remove(reason)
            listener.remove(this)
        }
    }

    override fun getBukkitLivingEntity(): CraftLivingEntity {
        val c = craftEntity
        return c ?: CraftLivingEntity(Bukkit.getServer() as CraftServer, this).apply {
            craftEntity = this
        }
    }
    override fun getBukkitEntity(): CraftEntity {
        val c = craftEntity
        return c ?: CraftLivingEntity(Bukkit.getServer() as CraftServer, this).apply {
            craftEntity = this
        }
    }

    private val dimensions = EntityDimensions(
        max(source.x(), source.z()).toFloat(),
        source.y().toFloat(),
        delegate.eyeHeight,
        EntityAttachments.createDefault(0F, 0F),
        true
    )

    override fun isDeadOrDying(): Boolean {
        return delegate.isDeadOrDying
    }

    override fun getDismountLocationForPassenger(passenger: LivingEntity): Vec3 {
        return delegate.getDismountLocationForPassenger(passenger)
    }

    override fun interact(player: Player, hand: InteractionHand): InteractionResult {
        val interact = ModelInteractEvent(player.bukkitEntity as org.bukkit.entity.Player, this, when (hand) {
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
        return delegate.addEffect(effectInstance, entity)
    }

    override fun addEffect(
        effectInstance: MobEffectInstance,
        entity: net.minecraft.world.entity.Entity?,
        cause: EntityPotionEffectEvent.Cause
    ): Boolean {
        return delegate.addEffect(effectInstance, entity, cause)
    }

    override fun addEffect(
        effectInstance: MobEffectInstance,
        entity: net.minecraft.world.entity.Entity?,
        cause: EntityPotionEffectEvent.Cause,
        fireEvent: Boolean
    ): Boolean {
        return delegate.addEffect(effectInstance, entity, cause, fireEvent)
    }

    override fun hurtClient(source: DamageSource): Boolean {
        return delegate.hurtClient(source)
    }

    override fun hurtServer(world: ServerLevel, source: DamageSource, amount: Float): Boolean {
        val ds = ModelDamageSourceImpl(source)
        val event = ModelDamagedEvent(this, ds, amount)
        if (!event.call()) return false
        if (listener.damage(ds, amount.toDouble())) return false
        return delegate.hurtServer(world, source, event.damage)
    }

    override fun deflection(projectile: Projectile): ProjectileDeflection {
        return delegate.deflection(projectile)
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

    override fun makeBoundingBox(vec3: Vec3): AABB {
        return if (!initialized) {
            super.makeBoundingBox(vec3)
        } else {
            AABB(
                vec3.x + source.minX,
                vec3.y + source.minY,
                vec3.z + source.minZ,
                vec3.x + source.maxX,
                vec3.y + source.maxY,
                vec3.z + source.maxZ
            )
        }
    }
    override fun getDefaultDimensions(pose: Pose): EntityDimensions = dimensions

    override fun remove() {
        remove(RemovalReason.KILLED)
    }
    override fun id(): Int = id
}