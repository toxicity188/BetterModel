/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.manager

import kr.toxicity.model.api.manager.PlayerManager
import kr.toxicity.model.api.nms.PlayerChannelHandler
import kr.toxicity.model.api.pack.PackZipper
import kr.toxicity.model.util.PLUGIN
import kr.toxicity.model.util.handleFailure
import kr.toxicity.model.util.registerListener
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object PlayerManagerImpl : PlayerManager, GlobalManager {

    private val playerMap = ConcurrentHashMap<UUID, PlayerChannelHandler>()

    override fun start() {
        registerListener(object : Listener {
            @EventHandler(priority = EventPriority.HIGHEST)
            fun PlayerJoinEvent.join() {
                if (player.isOnline) runCatching { //For fake player
                    player.register()
                }.handleFailure {
                    "Unable to load ${player.name}'s data."
                }
            }
            @EventHandler(priority = EventPriority.MONITOR)
            fun PlayerQuitEvent.quit() {
                playerMap.remove(player.uniqueId)?.use {
                    SkinManagerImpl.removeCache(it.base().profile())
                }
            }
        })
    }

    private fun Player.register() = playerMap.computeIfAbsent(uniqueId) {
        PLUGIN.nms().inject(this)
    }.apply {
        SkinManagerImpl.complete(base().profile().asUncompleted())
    }

    override fun reload(pipeline: ReloadPipeline, zipper: PackZipper) {
    }

    override fun end() {
        playerMap.values.removeIf {
            it.use { used -> SkinManagerImpl.removeCache(used.base().profile()) }
            true
        }
    }


    override fun player(uuid: UUID): PlayerChannelHandler? = playerMap[uuid]
    override fun player(player: Player): PlayerChannelHandler = player.register()
}
