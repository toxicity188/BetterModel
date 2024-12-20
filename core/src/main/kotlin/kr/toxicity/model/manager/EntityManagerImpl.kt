package kr.toxicity.model.manager

import kr.toxicity.model.api.manager.EntityManager
import kr.toxicity.model.api.tracker.EntityTracker
import kr.toxicity.model.util.registerListener
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent

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
        })
    }
}