/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.nms.v1_21_R4

import kr.toxicity.model.api.entity.BaseBukkitEntity
import kr.toxicity.model.api.entity.BaseBukkitPlayer
import kr.toxicity.model.api.nms.Profiled
import kr.toxicity.model.api.player.PlayerSkinParts
import kr.toxicity.model.api.profile.ModelProfile
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player
import java.util.stream.Stream

class BasePlayerImpl(
    private val delegate: CraftPlayer,
    private val profile: () -> ModelProfile,
    private val skinParts: () -> PlayerSkinParts
) : BaseBukkitEntity by BaseEntityImpl(delegate), BaseBukkitPlayer, Profiled by ProfiledImpl(PlayerArmorImpl(delegate), profile, skinParts) {
    override fun updateInventory() {
        delegate.handle.containerMenu.sendAllDataToRemote()
    }

    override fun trackedBy(): Stream<Player> = Stream.concat(
        Stream.of(delegate),
        delegate.trackedBy.stream()
    )
}
