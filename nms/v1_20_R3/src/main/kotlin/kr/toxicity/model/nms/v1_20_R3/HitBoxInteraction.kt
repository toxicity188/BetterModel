package kr.toxicity.model.nms.v1_20_R3

import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Interaction
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3

class HitBoxInteraction(
    val delegate: HitBoxImpl
) : Interaction(EntityType.INTERACTION, delegate.level()) {

    init {
        persist = false
    }

    override fun tick() {
        val dimension = delegate.getDimensions(Pose.STANDING)
        width = dimension.width
        height = dimension.height
        yRot = delegate.yRot
        xRot = delegate.xRot
        val pos = delegate.relativePosition()
        setPos(pos.x.toDouble(), pos.y.toDouble() - height / 2, pos.z.toDouble())
        setSharedFlagOnFire(delegate.remainingFireTicks > 0)
    }

    override fun skipAttackInteraction(entity: Entity): Boolean {
        return if (entity is Player) {
            entity.attack(delegate)
            true
        } else false
    }

    override fun interact(player: Player, hand: InteractionHand): InteractionResult {
        return delegate.interact(player, hand)
    }

    override fun interactAt(player: Player, vec: Vec3, hand: InteractionHand): InteractionResult {
        return delegate.interactAt(player, vec, hand)
    }
}