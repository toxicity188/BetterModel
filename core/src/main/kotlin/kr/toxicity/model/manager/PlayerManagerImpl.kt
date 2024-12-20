package kr.toxicity.model.manager

import kr.toxicity.model.api.manager.PlayerManager
import kr.toxicity.model.api.nms.PlayerChannelHandler
import kr.toxicity.model.util.PLUGIN
import kr.toxicity.model.util.registerListener
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object PlayerManagerImpl : PlayerManager, GlobalManagerImpl {

    private val playerMap = ConcurrentHashMap<UUID, PlayerChannelHandler>()

    override fun start() {
        registerListener(object : Listener {
            @EventHandler
            fun PlayerJoinEvent.join() {
                player.register()
            }
            @EventHandler
            fun PlayerQuitEvent.quit() {
                playerMap.remove(player.uniqueId)?.close()
            }
        })
    }

    private fun Player.register() = playerMap.computeIfAbsent(uniqueId) {
        PLUGIN.nms().inject(this)
    }

    override fun reload() {

    }

    override fun player(uuid: UUID): PlayerChannelHandler? = playerMap[uuid]
    override fun player(player: Player): PlayerChannelHandler = player.register()
}