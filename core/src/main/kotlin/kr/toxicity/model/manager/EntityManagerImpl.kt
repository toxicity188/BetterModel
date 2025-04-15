package kr.toxicity.model.manager

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent
import com.destroystokyo.paper.event.entity.EntityJumpEvent
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.animation.AnimationModifier
import kr.toxicity.model.api.event.ModelInteractEvent
import kr.toxicity.model.api.manager.EntityManager
import kr.toxicity.model.api.manager.ReloadInfo
import kr.toxicity.model.api.nms.HitBox
import kr.toxicity.model.api.tracker.EntityTracker
import kr.toxicity.model.util.registerListener
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.*
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent

object EntityManagerImpl : EntityManager, GlobalManagerImpl {
    override fun reload(info: ReloadInfo) {
        if (BetterModel.IS_PAPER) registerListener(object : Listener {
            @EventHandler(priority = EventPriority.MONITOR)
            fun EntityRemoveFromWorldEvent.remove() {
                EntityTracker.tracker(entity)?.let {
                    if (!it.forRemoval()) it.despawn()
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
        })
        registerListener(object : Listener {
            @EventHandler(priority = EventPriority.MONITOR)
            fun EntityRemoveEvent.remove() {
                EntityTracker.tracker(entity)?.let {
                    if (!it.forRemoval()) it.close()
                }
            }
            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            fun EntityPotionEffectEvent.potion() {
                EntityTracker.tracker(entity)?.forceUpdate(true)
            }
//            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
//            fun PlayerInteractAtEntityEvent.interactBukkit() {
//                if (hand != EquipmentSlot.HAND) return
//                val previous = player.vehicle
//                if (previous is HitBox && previous.source().uniqueId == rightClicked.uniqueId && previous.mountController().canDismountBySelf()) {
//                    previous.dismount(player)
//                }
//            }
            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            fun EntityDismountEvent.dismount() {
                val e = dismounted
                isCancelled = e is HitBox && (e.mountController().canFly() || !e.mountController().canDismountBySelf()) && !e.forceDismount()
            }
            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            fun ModelInteractEvent.interact() {
                if (hand == ModelInteractEvent.Hand.RIGHT) {
                    val previous = player.vehicle
                    if (previous is HitBox && previous.source().uniqueId == hitBox.source().uniqueId && previous.mountController().canDismountBySelf()) {
                        hitBox.dismount(player)
                    } else if (hitBox.mountController().canMount()) hitBox.mount(player)
                }
            }
            @EventHandler(priority = EventPriority.MONITOR)
            fun PlayerQuitEvent.quit() {
                EntityTracker.tracker(player)?.close()
            }
            @EventHandler(priority = EventPriority.MONITOR)
            fun ChunkLoadEvent.load() {
                chunk.entities.forEach {
                    EntityTracker.tracker(it)?.let {
                        it.refresh()
                        it.spawnNearby()
                    }
                }
            }
            @EventHandler(priority = EventPriority.MONITOR)
            fun ChunkUnloadEvent.unload() {
                chunk.entities.forEach {
                    EntityTracker.tracker(it)?.despawn()
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
                        it.close()
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
                    }) it.tint(it.damageTintValue())
                    else {
                        it.damageTint()
                    }
                }
            }
        })
    }
}