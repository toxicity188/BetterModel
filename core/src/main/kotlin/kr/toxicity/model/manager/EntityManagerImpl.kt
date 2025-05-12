package kr.toxicity.model.manager

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent
import com.destroystokyo.paper.event.entity.EntityJumpEvent
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.animation.AnimationModifier
import kr.toxicity.model.api.event.ModelInteractAtEvent
import kr.toxicity.model.api.manager.EntityManager
import kr.toxicity.model.api.manager.ReloadInfo
import kr.toxicity.model.api.nms.HitBox
import kr.toxicity.model.api.nms.ModelInteractionHand
import kr.toxicity.model.api.tracker.EntityTracker
import kr.toxicity.model.util.registerListener
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.*
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerPortalEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.inventory.EquipmentSlot.HAND
import org.bukkit.inventory.EquipmentSlot.OFF_HAND

object EntityManagerImpl : EntityManager, GlobalManagerImpl {

    override fun start() {
        if (BetterModel.IS_PAPER) registerListener(object : Listener { //More accurate world change event for Paper
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
                EntityTracker.tracker(entity)?.animate("jump")
            }
        })
        else registerListener(object : Listener { //Portal event for Spigot
            @EventHandler(priority = EventPriority.MONITOR)
            fun EntityRemoveEvent.remove() {
                EntityTracker.tracker(entity)?.let {
                    if (!it.forRemoval()) it.despawn()
                }
            }
            @EventHandler(priority = EventPriority.MONITOR)
            fun EntityPortalEvent.add() {
                EntityTracker.tracker(entity)?.let {
                    if (!it.forRemoval()) it.despawn()
                    if (!it.adapter.dead()) it.refresh()
                }
            }
            @EventHandler(priority = EventPriority.MONITOR)
            fun PlayerPortalEvent.add() {
                EntityTracker.tracker(player)?.let {
                    if (!it.forRemoval()) it.despawn()
                    if (!it.adapter.dead()) it.refresh()
                }
            }
        })
        registerListener(object : Listener {

            private val EntityEvent.entityTracker get() = EntityTracker.tracker(when (val e = entity) {
                is HitBox.Interaction -> e.sourceHitBox().source()
                is HitBox -> e.source()
                else -> e
            })

            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            fun EntityPotionEffectEvent.potion() { //Apply potion effect
                entityTracker?.forceUpdate(true)
            }
            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            fun EntityDismountEvent.dismount() { //Dismount
                val e = dismounted
                isCancelled = e is HitBox && (e.mountController().canFly() || !e.mountController().canDismountBySelf()) && !e.forceDismount()
            }
            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            fun ModelInteractAtEvent.interact() { //Mount
                if (hand == ModelInteractionHand.RIGHT) {
                    val previous = player.vehicle
                    if (previous is HitBox && previous.source().uniqueId == hitBox.source().uniqueId && previous.mountController().canDismountBySelf()) {
                        hitBox.dismount(player)
                    } else if (hitBox.mountController().canMount()) hitBox.mount(player)
                }
            }
            @EventHandler(priority = EventPriority.MONITOR)
            fun PlayerQuitEvent.quit() { //Quit
                EntityTracker.tracker(player)?.close()
                (player.vehicle as? HitBox)?.dismount(player)
            }
            @EventHandler(priority = EventPriority.MONITOR)
            fun ChunkLoadEvent.load() { //Chunk load
                chunk.entities.forEach {
                    EntityTracker.tracker(it)?.refresh()
                }
            }
            @EventHandler(priority = EventPriority.MONITOR)
            fun ChunkUnloadEvent.unload() { //Chunk unload
                chunk.entities.forEach {
                    EntityTracker.tracker(it)?.despawn()
                }
            }
            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            fun EntityDeathEvent.death() { //Death
                EntityTracker.tracker(entity)?.let {
                    if (!it.animate("death", AnimationModifier.DEFAULT_WITH_PLAY_ONCE) {
                            it.close()
                        }) it.close()
                    else it.forRemoval(true)
                }
            }
            @EventHandler(priority = EventPriority.MONITOR)
            fun PlayerInteractAtEntityEvent.interact() {
                (rightClicked as? HitBox.Interaction)?.sourceHitBox()?.triggerInteractAt(
                    player,
                    when (this.hand) {
                        HAND -> ModelInteractionHand.RIGHT
                        OFF_HAND -> ModelInteractionHand.LEFT
                        else -> return
                    },
                    clickedPosition
                )
            }
            @EventHandler(priority = EventPriority.MONITOR)
            fun PlayerInteractEntityEvent.interact() { //Interact base entity based on interaction entity
                (rightClicked as? HitBox.Interaction)?.sourceHitBox()?.triggerInteract(
                    player,
                    when (this.hand) {
                        HAND -> ModelInteractionHand.RIGHT
                        OFF_HAND -> ModelInteractionHand.LEFT
                        else -> return
                    }
                )
            }
            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            fun EntityDamageEvent.damage() { //Damage
                if (this is EntityDamageByEntityEvent) {
                    val victim = entity.run {
                        if (this is HitBox) source().uniqueId else uniqueId
                    }
                    val v = damager.vehicle
                    if (v is HitBox && !v.mountController().canBeDamagedByRider() && v.source().uniqueId == victim) {
                        isCancelled = true
                        return
                    }
                }
                EntityTracker.tracker(entity)?.let {
                    if (it.animate("damage", AnimationModifier.DEFAULT_WITH_PLAY_ONCE) {
                            it.tint(0xFFFFFF)
                        }) it.tint(it.damageTintValue())
                    else {
                        it.damageTint()
                    }
                }
            }
        })
    }

    override fun reload(info: ReloadInfo) {
    }
}