package kr.toxicity.model.manager

import kr.toxicity.model.api.manager.EntityManager
import kr.toxicity.model.api.tracker.EntityTracker
import kr.toxicity.model.util.PLUGIN
import kr.toxicity.model.util.registerListener
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import java.util.concurrent.TimeUnit

object EntityManagerImpl : EntityManager, GlobalManagerImpl {
    override fun reload() {
        registerListener(object : Listener {
            @EventHandler
            fun EntityDeathEvent.death() {
                EntityTracker.tracker(entity)?.let {
                    if (!it.animateSingle("death", { true }) {
                        it.close()
                    }) it.close()
                }
            }
            @EventHandler
            fun EntityDamageByEntityEvent.attack() {
                EntityTracker.tracker(damager)?.animateSingle("attack")
            }
            @EventHandler(ignoreCancelled = false)
            fun EntityDamageEvent.damage() {
                val e = entity
                if (e is LivingEntity) EntityTracker.tracker(e)?.let {
                    if (it.animateSingle("damage", { true }) {
                        it.setColor(Color.WHITE)
                    }) it.setColor(null)
                    else {
                        it.setColor(null)
                        Bukkit.getAsyncScheduler().runDelayed(PLUGIN, { _ ->
                            it.setColor(Color.WHITE)
                        }, e.maximumNoDamageTicks.toLong() * 25 , TimeUnit.MILLISECONDS)
                    }
                }
            }
        })
    }
}