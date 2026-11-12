/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.bukkit.nms.v26_R3

import kr.toxicity.model.api.animation.keyframe.KeyframeRuntimeContext
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

    override fun updateTo(context: KeyframeRuntimeContext) {
        BaseEntityImpl.registerEntityQueries(delegate, context)
        context.setFunction("is_spectator") { if (delegate.handle.isSpectator) 1F else 0F }
        context.setFunction("player_level") { delegate.level.toFloat() }
        context.setFunction("cardinal_facing") { delegate.handle.nearestViewDirection.get3DDataValue().toFloat() }
        context.setFunction("cardinal_player_facing") { delegate.handle.nearestViewDirection.get3DDataValue().toFloat() }
        context.setFunction("cardinal_facing_2d") { delegate.handle.direction.get3DDataValue().toFloat() }
    }
}
