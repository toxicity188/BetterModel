package kr.toxicity.model.manager

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent
import com.destroystokyo.paper.event.entity.EntityJumpEvent
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.data.renderer.AnimationModifier
import kr.toxicity.model.api.event.ModelInteractEvent
import kr.toxicity.model.api.manager.EntityManager
import kr.toxicity.model.api.nms.HitBox
import kr.toxicity.model.api.tracker.EntityTracker
import kr.toxicity.model.util.PLUGIN
import kr.toxicity.model.util.registerListener
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityDismountEvent
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.entity.EntityTeleportEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.inventory.EquipmentSlot

object EntityManagerImpl : EntityManager, GlobalManagerImpl {
    override fun reload() {
        if (BetterModel.IS_PAPER) registerListener(object : Listener {
            @EventHandler(priority = EventPriority.MONITOR)
            fun EntityRemoveFromWorldEvent.remove() {
                EntityTracker.tracker(entity)?.let {
                    if (!it.forRemoval()) it.close()
                }
            }
            @EventHandler(priority = EventPriority.MONITOR)
            fun EntityAddToWorldEvent.add() {
                EntityTracker.tracker(entity)?.refresh()
            }
            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            fun EntityJumpEvent.jump() {
                EntityTracker.tracker(entity)?.animateSingle("jump")
            }
        }) else registerListener(object : Listener {
            @EventHandler(priority = EventPriority.MONITOR)
            @Suppress("DEPRECATION")
            fun org.bukkit.event.entity.EntityRemoveEvent.remove() {
                EntityTracker.tracker(entity)?.let {
                    if (!it.forRemoval()) it.close()
                }
            }
            @EventHandler(priority = EventPriority.MONITOR)
            fun EntitySpawnEvent.add() {
                EntityTracker.tracker(entity)?.refresh()
            }
        })

        registerListener(object : Listener {
            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            fun EntityPotionEffectEvent.potion() {
                EntityTracker.tracker(entity)?.forceUpdate(true)
            }
            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            fun PlayerInteractAtEntityEvent.interactBukkit() {
                if (hand != EquipmentSlot.HAND) return
                val previous = player.vehicle
                if (previous is HitBox && previous.source().uniqueId == rightClicked.uniqueId) {
                    previous.dismount(player)
                }
            }
            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            fun EntityDismountEvent.dismount() {
                val e = dismounted
                isCancelled = e is HitBox && e.mountController().canFly() && !e.forceDismount()
            }
            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            fun ModelInteractEvent.interact() {
                if (hand == ModelInteractEvent.Hand.RIGHT) {
                    val previous = player.vehicle
                    if (previous is HitBox && previous.source().uniqueId == hitBox.source().uniqueId) {
                        hitBox.dismount(player)
                    } else if (hitBox.mountController().canMount()) hitBox.mount(player)
                }
            }
            @EventHandler(priority = EventPriority.MONITOR)
            fun PlayerQuitEvent.quit() {
                EntityTracker.tracker(player.uniqueId)?.close()
            }
            @EventHandler(priority = EventPriority.MONITOR)
            fun ChunkLoadEvent.load() {
                chunk.entities.forEach {
                    EntityTracker.tracker(it)?.refresh()
                }
            }
            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            fun EntityTeleportEvent.teleport() {
                EntityTracker.tracker(entity)?.refresh()
            }
            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
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
            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
            fun EntityDamageEvent.damage() {
                val e = entity
                if (e !is LivingEntity) return
                EntityTracker.tracker(e)?.let {
                    if (it.animateSingle("damage", AnimationModifier.DEFAULT) {
                        it.tint(0xFFFFFF)
                    }) it.tint(0xFF7878)
                    else {
                        it.damageTint()
                    }
                }
            }
        })
    }
}