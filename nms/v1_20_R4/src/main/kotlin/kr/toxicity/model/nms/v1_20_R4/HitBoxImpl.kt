package kr.toxicity.model.nms.v1_20_R4

import kr.toxicity.model.api.data.blueprint.ModelBoundingBox
import kr.toxicity.model.api.event.ModelDamagedEvent
import kr.toxicity.model.api.event.ModelInteractEvent
import kr.toxicity.model.api.event.ModelInteractEvent.Hand
import kr.toxicity.model.api.nms.HitBox
import kr.toxicity.model.api.nms.HitBoxListener
import kr.toxicity.model.api.nms.TransformSupplier
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionHand.*
import net.minecraft.world.InteractionResult
import net.minecraft.world.damagesource.DamageSource
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
import org.bukkit.craftbukkit.damage.CraftDamageSource
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.craftbukkit.entity.CraftLivingEntity
import org.bukkit.entity.Entity
import org.joml.Vector3f

class HitBoxImpl(
    private val name: String,
    private val source: ModelBoundingBox,
    private val supplier: TransformSupplier,
    private val listener: HitBoxListener,
    private val delegate: LivingEntity
) : LivingEntity(EntityType.SLIME, delegate.level()), HitBox {
    private var initialized = false

    init {
        moveTo(delegate.position())
        isInvisible = true
        persist = false
        isSilent = true
        initialized = true
        updatingSectionStatus = false
    }

    override fun name(): String = name
    override fun source(): Entity = delegate.bukkitLivingEntity
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
        bukkitEntity.addPassenger(entity)
    }

    override fun tick() {
        val transform = supplier.supplyTransform()
        setPos(delegate.position().add(
            transform.x.toDouble(),
            transform.y.toDouble() + delegate.passengerPosition().y,
            transform.z.toDouble()
        ))
        listener.sync(this)
    }

    override fun remove(reason: RemovalReason) {
        super.remove(reason)
        listener.remove(this)
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

    override fun getYRot(): Float {
        return if (!initialized) super.getYRot() else delegate.yRot
    }

    override fun getXRot(): Float {
        return if (!initialized) super.getXRot() else delegate.xRot
    }

    override fun getYHeadRot(): Float {
        return if (!initialized) super.getYHeadRot() else delegate.getYHeadRot()
    }

    private val dimensions = EntityDimensions(
        0F,
        0F,
        0F,
        EntityAttachments.createDefault(0F, 0F),
        true
    )

    override fun isDeadOrDying(): Boolean {
        return delegate.isDeadOrDying
    }

    override fun getHealth(): Float {
        return delegate.health
    }

    override fun getDismountLocationForPassenger(passenger: LivingEntity): Vec3 {
        return delegate.getDismountLocationForPassenger(passenger)
    }

    override fun interact(player: Player, hand: InteractionHand): InteractionResult {
        val interact = ModelInteractEvent(player.bukkitEntity as org.bukkit.entity.Player, this, when (hand) {
            MAIN_HAND -> Hand.RIGHT
            OFF_HAND -> Hand.LEFT
        })
        if (!interact.callEvent()) return InteractionResult.FAIL
        return delegate.interact(player, hand)
    }

    override fun hurt(source: DamageSource, amount: Float): Boolean {
        val ds = CraftDamageSource(source)
        val event = ModelDamagedEvent(this, ds, amount)
        if (!event.callEvent()) return false
        if (listener.damage(ds, amount.toDouble())) return false
        return delegate.hurt(source, event.damage)
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

    override fun makeBoundingBox(): AABB {
        return if (!initialized) {
            super.makeBoundingBox()
        } else {
            val pos = position()
            AABB(
                pos.x + source.minX,
                pos.y + source.minY,
                pos.z + source.minZ,
                pos.x + source.maxX,
                pos.y + source.maxY,
                pos.z + source.maxZ
            )
        }
    }

    override fun getDefaultDimensions(pose: Pose): EntityDimensions = dimensions

    override fun remove() {
        remove(RemovalReason.KILLED)
    }
    override fun id(): Int = id
}