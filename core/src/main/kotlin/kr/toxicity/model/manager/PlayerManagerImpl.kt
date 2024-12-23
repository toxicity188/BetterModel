package kr.toxicity.model.manager

import kr.toxicity.model.api.manager.PlayerManager
import kr.toxicity.model.api.nms.PlayerChannelHandler
import kr.toxicity.model.api.tracker.EntityTracker
import kr.toxicity.model.api.util.EntityUtil
import kr.toxicity.model.util.PLUGIN
import kr.toxicity.model.util.registerListener
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent
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
                player.showAll()
            }
            @EventHandler
            fun PlayerChangedWorldEvent.change() {
                player.register().unregisterAll()
                player.showAll()
            }
            @EventHandler
            fun PlayerQuitEvent.quit() {
                playerMap.remove(player.uniqueId)?.close()
            }
        })
    }

    private fun Player.showAll() {
        val loc = location
        Bukkit.getRegionScheduler().runDelayed(PLUGIN, loc, {
            loc.getNearbyLivingEntities(EntityUtil.RENDER_DISTANCE).forEach {
                EntityTracker.tracker(it)?.spawn(this)
            }
        }, 10)
    }

    private fun Player.register() = playerMap.computeIfAbsent(uniqueId) {
        PLUGIN.nms().inject(this)
    }

    override fun reload() {

    }

    override fun player(uuid: UUID): PlayerChannelHandler? = playerMap[uuid]
    override fun player(player: Player): PlayerChannelHandler = player.register()
}