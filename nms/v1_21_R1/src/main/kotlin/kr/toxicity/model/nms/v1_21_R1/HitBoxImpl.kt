package kr.toxicity.model.nms.v1_21_R1

import kr.toxicity.model.api.event.ModelDamagedEvent
import kr.toxicity.model.api.nms.HitBox
import kr.toxicity.model.api.nms.HitBoxListener
import kr.toxicity.model.api.nms.TransformSupplier
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.*
import net.minecraft.world.entity.player.Player
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
    private val source: AABB,
    private val supplier: TransformSupplier,
    private val listener: HitBoxListener,
    private val delegate: LivingEntity,
    private val onRemove: (HitBoxImpl) -> Unit
) : LivingEntity(EntityType.SLIME, delegate.level()), HitBox {
    private var initialized = false

    init {
        moveTo(delegate.position())
        isInvisible = true
        persist = false
        isSilent = true
        isCollidable(false)
        pose = Pose.STANDING
        initialized = true
        `moonrise$setUpdatingSectionStatus`(false)
    }

    override fun name(): String = name
    override fun source(): Entity = delegate.bukkitLivingEntity
    override fun entity(): Entity = bukkitEntity
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

    override fun tick() {
        listener.sync(this)
    }

    override fun remove(reason: RemovalReason) {
        super.remove(reason)
        listener.remove(this)
        onRemove(this)
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

    override fun position(): Vec3 {
        val transform = supplier.supplyTransform()
        return delegate.position().add(
            transform.x.toDouble(),
            transform.y.toDouble() - (source.maxY - source.minY) / 2,
            transform.z.toDouble()
        )
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
        return delegate.interact(player, hand)
    }

    override fun hurt(source: DamageSource, amount: Float): Boolean {
        if (listener.damage(CraftDamageSource(source), amount.toDouble())) return false
        return delegate.hurt(source, amount).also {
            if (it) ModelDamagedEvent(this).callEvent()
        }
    }

    override fun makeBoundingBox(): AABB {
        return if (!initialized) {
            super.makeBoundingBox()
        } else dimensions.makeBoundingBox(delegate.position()) + source
    }

    override fun getDefaultDimensions(pose: Pose): EntityDimensions = dimensions

    override fun remove() {
        remove(RemovalReason.KILLED)
    }
    override fun id(): Int = id
}