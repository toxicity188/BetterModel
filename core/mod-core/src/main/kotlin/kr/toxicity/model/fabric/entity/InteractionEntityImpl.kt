/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.fabric.entity

import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Interaction
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3

class InteractionEntityImpl(val delegate: HitBoxEntityImpl) :
    Interaction(EntityType.INTERACTION, delegate.level())
{
    override fun tick() {
        delegate.calculateDimensions().let { dimensions ->
            width = dimensions.width
            height = dimensions.height
        }

        yRot = delegate.yRot
        xRot = delegate.xRot
        setSharedFlagOnFire(delegate.remainingFireTicks > 0)
    }

    override fun skipAttackInteraction(entity: Entity): Boolean {
        if (entity !is Player) {
            return false
        }

        entity.attack(delegate)
        return true
    }

    override fun interact(player: Player, hand: InteractionHand): InteractionResult {
        return InteractionResult.FAIL
    }

    override fun interactAt(player: Player, vec: Vec3, hand: InteractionHand): InteractionResult {
        return InteractionResult.FAIL
    }

    override fun shouldBeSaved(): Boolean = false
}
