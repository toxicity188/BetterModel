/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.impl.fabric.manager

import kr.toxicity.model.api.manager.PlayerManager
import kr.toxicity.model.api.nms.PlayerChannelHandler
import kr.toxicity.model.api.pack.PackZipper
import kr.toxicity.model.api.platform.PlatformPlayer
import kr.toxicity.model.impl.fabric.wrap
import kr.toxicity.model.manager.GlobalManager
import kr.toxicity.model.manager.ReloadPipeline
import kr.toxicity.model.manager.SkinManagerImpl
import kr.toxicity.model.util.PLATFORM
import kr.toxicity.model.util.handleFailure
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.minecraft.server.level.ServerPlayer
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object PlayerManagerImpl : PlayerManager, GlobalManager {
    private val playerMap = ConcurrentHashMap<UUID, PlayerChannelHandler>()

    override fun start() {
        ServerPlayerEvents.JOIN.register { handleJoin(it) }
        ServerPlayerEvents.LEAVE.register { handleLeave(it) }
    }

    private fun handleJoin(player: ServerPlayer) {
        runCatching {
            player.connection.wrap().register()
        }.handleFailure {
            "Unable to load ${player.name}'s data."
        }
    }

    private fun handleLeave(player: ServerPlayer) {
        playerMap.remove(player.uuid)?.use {
            SkinManagerImpl.removeCache(it.base().profile())
        }
    }

    override fun end() {
        playerMap.values.forEach { handler ->
            handler.use { used ->
                SkinManagerImpl.removeCache(used.base().profile())
            }
        }

        playerMap.clear()
    }

    override fun player(uuid: UUID): PlayerChannelHandler? = playerMap[uuid]

    override fun player(player: PlatformPlayer): PlayerChannelHandler = player.register()

    private fun PlatformPlayer.register(): PlayerChannelHandler {
        return playerMap.computeIfAbsent(uuid()) {
            PLATFORM.nms().inject(this)
        }.apply {
            SkinManagerImpl.complete(base().profile().asUncompleted())
        }
    }

    override fun reload(pipeline: ReloadPipeline, zipper: PackZipper) = Unit
}
