/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.bukkit.nms.v1_21_R3

import kr.toxicity.model.api.bukkit.entity.BaseBukkitEntity
import kr.toxicity.model.api.bukkit.entity.BaseBukkitPlayer
import kr.toxicity.model.api.nms.Profiled
import kr.toxicity.model.api.platform.PlatformPlayer
import kr.toxicity.model.api.player.PlayerSkinParts
import kr.toxicity.model.api.profile.ModelProfile
import net.minecraft.util.Mth
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player
import java.util.stream.Stream

internal data class BasePlayerImpl(
    private val delegate: CraftPlayer,
    private val profile: () -> ModelProfile,
    private val skinParts: () -> PlayerSkinParts
) : BaseBukkitEntity by BaseEntityImpl(delegate), BaseBukkitPlayer, Profiled by ProfiledImpl(PlayerArmorImpl(delegate), profile, skinParts) {

    override fun entity(): Player = delegate

    override fun updateInventory() {
        delegate.handle.containerMenu.sendAllDataToRemote()
    }

    override fun platform(): PlatformPlayer = delegate.wrap()

    override fun trackedBy(): Stream<PlatformPlayer> = Stream.concat(
        Stream.of(delegate),
        delegate.trackedBy.stream()
    ).map {
        it.wrap()
    }

    override fun bodyYaw(): Float {
        val handle = delegate.handle
        var yaw = -45 * handle.xMovement()
        if (handle.zMovement() < 0) yaw *= -1
        return Mth.wrapDegrees(handle.yHeadRot + yaw)
    }
}

