package kr.toxicity.model.nms.v1_21_R3

import kr.toxicity.model.api.data.blueprint.ModelBoundingBox
import kr.toxicity.model.api.event.ModelDamagedEvent
import kr.toxicity.model.api.nms.HitBox
import kr.toxicity.model.api.nms.HitBoxListener
import kr.toxicity.model.api.nms.TransformSupplier
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.*
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
    private val delegate: LivingEntity,
    private val onRemove: (HitBoxImpl) -> Unit
) : LivingEntity(EntityType.SLIME, delegate.level()), HitBox {
    private var initialized = false

    init {
        moveTo(delegate.position())
        isInvisible = true
        persist = false
        isSilent = true
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

    override fun hurtClient(source: DamageSource): Boolean {
        return delegate.hurtClient(source)
    }

    override fun hurtServer(world: ServerLevel, source: DamageSource, amount: Float): Boolean {
        if (listener.damage(CraftDamageSource(source), amount.toDouble())) return false
        return delegate.hurtServer(world, source, amount).also {
            if (it) ModelDamagedEvent(this).callEvent()
        }
    }

    override fun deflection(projectile: Projectile): ProjectileDeflection {
        return delegate.deflection(projectile)
    }

    override fun makeBoundingBox(vec3: Vec3): AABB {
        return if (!initialized) {
            super.makeBoundingBox()
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