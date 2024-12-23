package kr.toxicity.model.nms.v1_21_R1

import com.google.common.collect.ImmutableList
import kr.toxicity.model.api.nms.HitBox
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.*
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.phys.AABB
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.CraftServer
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.craftbukkit.entity.CraftLivingEntity

class HitBoxImpl(
    private val source: AABB,
    private val delegate: LivingEntity,
    private val onRemove: (HitBoxImpl) -> Unit
) : LivingEntity(EntityType.SLIME, delegate.level()), HitBox {

    private var initialized = false

    init {
        moveTo(delegate.position())
        isInvisible = true
        isInvulnerable = true
        persist = false
        isSilent = true
        isCollidable(false)
        pose = Pose.STANDING
        initialized = true
    }

    private var craftEntity: CraftLivingEntity? = null
    override fun getArmorSlots(): MutableIterable<ItemStack> = mutableSetOf()
    override fun getItemBySlot(slot: EquipmentSlot): ItemStack = Items.AIR.defaultInstance
    override fun setItemSlot(slot: EquipmentSlot, stack: ItemStack) {
    }
    override fun getMainArm(): HumanoidArm = HumanoidArm.RIGHT

    override fun tick() {
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
        0F,
        0F,
        0F,
        EntityAttachments.createDefault(0F, 0F),
        true
    )

    override fun remove(reason: RemovalReason) {
        super.remove(reason)
        onRemove(this)
    }

    override fun isDeadOrDying(): Boolean {
        return delegate.isDeadOrDying
    }

    override fun getHealth(): Float {
        return delegate.getHealth()
    }

    override fun getDismountPoses(): ImmutableList<Pose> {
        return delegate.dismountPoses
    }

    override fun interact(player: Player, hand: InteractionHand): InteractionResult {
        return delegate.interact(player, hand)
    }

    override fun hurt(source: DamageSource, amount: Float): Boolean {
        return delegate.hurt(source, amount)
    }

    override fun makeBoundingBox(): AABB {
        return if (!initialized) {
            super.makeBoundingBox()
        } else dimensions.makeBoundingBox(delegate.position()) + source
    }

    override fun getDefaultDimensions(pose: Pose): EntityDimensions = dimensions

    private operator fun AABB.plus(other: AABB): AABB = AABB(
        minX + other.minX,
        minX + other.minY,
        minZ + other.minZ,
        maxX + other.maxX,
        maxY + other.maxY,
        maxZ + other.maxZ
    )

    override fun remove() {
        remove(RemovalReason.KILLED)
    }
    override fun id(): Int = id
}