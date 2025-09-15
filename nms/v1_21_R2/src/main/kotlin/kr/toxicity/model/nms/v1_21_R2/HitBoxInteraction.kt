/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.nms.v1_21_R2

import kr.toxicity.model.api.nms.HitBox
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Interaction
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.CraftServer
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.craftbukkit.entity.CraftInteraction

internal class HitBoxInteraction(
    val delegate: HitBoxImpl
) : Interaction(EntityType.INTERACTION, delegate.level()) {

    init {
        persist = false
    }

    private val craftEntity: CraftInteraction by lazy {
        object : CraftInteraction(Bukkit.getServer() as CraftServer, this), HitBox by delegate {}
    }

    override fun getBukkitEntity(): CraftEntity = craftEntity
    override fun getBukkitEntityRaw(): CraftEntity = craftEntity
    override fun hasExactlyOnePlayerPassenger(): Boolean = false

    override fun tick() {
        val dimension = delegate.dimensions
        width = dimension.width
        height = dimension.height
        yRot = delegate.yRot
        xRot = delegate.xRot
        setSharedFlagOnFire(delegate.remainingFireTicks > 0)
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