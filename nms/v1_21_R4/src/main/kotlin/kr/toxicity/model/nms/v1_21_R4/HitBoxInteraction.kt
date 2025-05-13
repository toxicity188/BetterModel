package kr.toxicity.model.nms.v1_21_R4

import kr.toxicity.model.api.nms.HitBox
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Interaction
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.CraftServer
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.craftbukkit.entity.CraftInteraction

class HitBoxInteraction(
    val delegate: HitBoxImpl
) : Interaction(EntityType.INTERACTION, delegate.level()), HitBox.Interaction {

    companion object {
        val serializers = Interaction::class.java.declaredFields.filter { f ->
            EntityDataAccessor::class.java.isAssignableFrom(f.type)
        }.map {
            it.isAccessible = true
            it.get(null) as EntityDataAccessor<*>
        }
    }

    init {
        persist = false
    }

    private val craftEntity: CraftInteraction by lazy {
        object : CraftInteraction(Bukkit.getServer() as CraftServer, this), HitBox.Interaction by this {}
    }

    override fun getBukkitEntity(): CraftEntity = craftEntity
    override fun getBukkitEntityRaw(): CraftEntity = craftEntity
    override fun sourceHitBox(): HitBox = delegate.craftEntity

    override fun tick() {
        val dimension = delegate.getDimensions(Pose.STANDING)
        width = dimension.width
        height = dimension.height
        yRot = delegate.yRot
        xRot = delegate.xRot
        val pos = delegate.relativePosition()
        setPos(pos.x.toDouble(), pos.y.toDouble() - height / 2, pos.z.toDouble())
        setSharedFlagOnFire(delegate.remainingFireTicks > 0)
        serializers.forEach {
            entityData.markDirty(it)
        }
    }

    override fun skipAttackInteraction(entity: Entity): Boolean {
        return if (entity is Player) {
            entity.attack(delegate)
            true
        } else false
    }

    override fun interact(player: Player, hand: InteractionHand): InteractionResult {
        return InteractionResult.FAIL
    }

    override fun interactAt(player: Player, vec: Vec3, hand: InteractionHand): InteractionResult {
        return InteractionResult.FAIL
    }
}