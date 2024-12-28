package kr.toxicity.model.manager

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.data.renderer.AnimationModifier
import kr.toxicity.model.api.event.ModelInteractEvent
import kr.toxicity.model.api.manager.EntityManager
import kr.toxicity.model.api.tracker.EntityTracker
import kr.toxicity.model.util.PLUGIN
import kr.toxicity.model.util.registerListener
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.world.ChunkLoadEvent

object EntityManagerImpl : EntityManager, GlobalManagerImpl {
    override fun reload() {
        if (BetterModel.IS_PAPER) registerListener(object : Listener {
            @EventHandler
            fun EntityRemoveFromWorldEvent.remove() {
                EntityTracker.tracker(entity)?.let {
                    if (!it.forRemoval()) it.close()
                }
            }
            @EventHandler
            fun EntityAddToWorldEvent.add() {
                EntityTracker.tracker(entity)?.refresh()
            }
        }) else registerListener(object : Listener {
            @EventHandler
            @Suppress("removal", "DEPRECATION")
            fun org.bukkit.event.entity.EntityRemoveEvent.remove() {
                EntityTracker.tracker(entity)?.let {
                    if (!it.forRemoval()) it.close()
                }
            }
            @EventHandler
            fun EntitySpawnEvent.add() {
                EntityTracker.tracker(entity)?.refresh()
            }
        })

        registerListener(object : Listener {
            @EventHandler
            fun ModelInteractEvent.interact() {
                if (hitBox.name().startsWith("p_")) {
                    hitBox.addPassenger(player)
                }
            }
            @EventHandler
            fun PlayerQuitEvent.quit() {
                EntityTracker.tracker(player.uniqueId)?.close()
            }
            @EventHandler
            fun ChunkLoadEvent.load() {
                chunk.entities.forEach {
                    EntityTracker.tracker(it)?.refresh()
                }
            }
            @EventHandler
            fun EntityDeathEvent.death() {
                EntityTracker.tracker(entity)?.let {
                    if (!it.animateSingle("death", AnimationModifier.DEFAULT) {
                        PLUGIN.scheduler().task(entity.location) {
                            it.close()
                        }
                    }) it.close()
                    else it.forRemoval(true)
                }
            }
            @EventHandler(ignoreCancelled = false)
            fun EntityDamageEvent.damage() {
                val e = entity
                if (e is LivingEntity) EntityTracker.tracker(e)?.let {
                    if (it.animateSingle("damage", AnimationModifier.DEFAULT) {
                        it.tint(false)
                    }) it.tint(true)
                    else {
                        it.tint(true)
                        PLUGIN.scheduler().asyncTaskLater(e.maximumNoDamageTicks.toLong() / 2) {
                            it.tint(false)
                        }
                    }
                }
            }
        })
    }
}