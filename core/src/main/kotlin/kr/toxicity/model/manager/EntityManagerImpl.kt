package kr.toxicity.model.manager

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent
import com.destroystokyo.paper.event.entity.EntityJumpEvent
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.animation.AnimationModifier
import kr.toxicity.model.api.manager.EntityManager
import kr.toxicity.model.api.manager.ReloadInfo
import kr.toxicity.model.api.nms.HitBox
import kr.toxicity.model.api.nms.ModelInteractionHand
import kr.toxicity.model.api.tracker.EntityTracker
import kr.toxicity.model.util.registerListener
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
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
import org.bukkit.inventory.EquipmentSlot

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

            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            fun EntityPotionEffectEvent.potion() { //Apply potion effect
                EntityTracker.tracker(entity)?.updateBaseEntity()
            }
            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            fun EntityDismountEvent.dismount() { //Dismount
                val e = dismounted
                isCancelled = e is HitBox && (e.mountController().canFly() || !e.mountController().canDismountBySelf()) && !e.forceDismount()
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
                (rightClicked as? HitBox)?.triggerInteractAt(
                    player,
                    when (this.hand) {
                        EquipmentSlot.HAND -> ModelInteractionHand.RIGHT
                        EquipmentSlot.OFF_HAND -> ModelInteractionHand.LEFT
                        else -> return
                    },
                    clickedPosition
                )
            }

            @EventHandler(priority = EventPriority.MONITOR)
            fun PlayerInteractEntityEvent.interact() { //Interact base entity based on interaction entity
                val isRight = hand == EquipmentSlot.HAND
                val dismount = isRight && player.triggerDismount(rightClicked)
                (rightClicked as? HitBox)?.let {
                    it.triggerInteract(
                        player,
                        when (this.hand) {
                            EquipmentSlot.HAND -> ModelInteractionHand.RIGHT
                            EquipmentSlot.OFF_HAND -> ModelInteractionHand.LEFT
                            else -> return
                        }
                    )
                    if (isRight && !dismount) player.triggerMount(it)
                }
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
//                    if (cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
//                        EntityTracker.tracker(damager)?.animate("attack", AnimationModifier.DEFAULT_WITH_PLAY_ONCE)
//                    }
                }
                EntityTracker.tracker(entity)?.let {
                    it.animate("damage", AnimationModifier.DEFAULT_WITH_PLAY_ONCE)
                    it.damageTint()
                }
            }
        })
    }

    private fun Player.triggerDismount(e: Entity): Boolean {
        val previous = vehicle
        if (previous !is HitBox) return false
        val uuid = if (e is HitBox) e.source().uniqueId else e.uniqueId
        if (previous.source().uniqueId == uuid && previous.mountController().canDismountBySelf()) {
            previous.dismount(this)
            return true
        }
        return false
    }

    private fun Player.triggerMount(hitBox: HitBox) {
        if (hitBox.mountController().canMount()) hitBox.mount(this)
    }

    override fun reload(info: ReloadInfo) {
    }
}