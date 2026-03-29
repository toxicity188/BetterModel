/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.impl.fabric.entity

import kr.toxicity.model.api.mod.entity.BaseModEntity
import kr.toxicity.model.api.mod.entity.BaseModPlayer
import kr.toxicity.model.api.nms.Profiled
import kr.toxicity.model.api.platform.PlatformPlayer
import kr.toxicity.model.api.player.PlayerSkinParts
import kr.toxicity.model.api.profile.ModelProfile
import kr.toxicity.model.impl.fabric.armor.PlayerArmorImpl
import kr.toxicity.model.impl.fabric.seenBy
import kr.toxicity.model.impl.fabric.wrap
import kr.toxicity.model.impl.fabric.xMovement
import kr.toxicity.model.impl.fabric.zMovement
import net.minecraft.server.network.ServerPlayerConnection
import net.minecraft.util.Mth
import java.util.stream.Stream

class BaseFabricPlayerImpl(
    private val connection: ServerPlayerConnection,
    private val profile: () -> ModelProfile,
    private val skinParts: () -> PlayerSkinParts
) : BaseModPlayer, BaseModEntity by BaseFabricEntityImpl(connection.player), Profiled by ProfiledImpl(PlayerArmorImpl(connection), profile, skinParts) {

    override fun updateInventory() {
        connection.player.containerMenu.sendAllDataToRemote()
    }

    override fun platform(): PlatformPlayer = connection.wrap()

    override fun trackedBy(): Stream<PlatformPlayer> = Stream.concat(
        Stream.of(connection),
        connection.player.seenBy.stream()
    ).map {
        it.wrap()
    }

    override fun bodyYaw(): Float {
        val handle = connection.player
        var yaw = -45 * handle.xMovement()
        if (handle.zMovement() < 0) yaw *= -1
        return Mth.wrapDegrees(handle.yHeadRot + yaw)
    }
}
