package kr.toxicity.model.manager

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent
import com.destroystokyo.paper.event.entity.EntityJumpEvent
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.animation.AnimationModifier
import kr.toxicity.model.api.manager.EntityManager
import kr.toxicity.model.api.manager.ReloadInfo
import kr.toxicity.model.api.nms.HitBox
import kr.toxicity.model.api.nms.ModelInteractionHand
import kr.toxicity.model.api.pack.PackZipper
import kr.toxicity.model.api.tracker.EntityTracker
import kr.toxicity.model.api.tracker.EntityTrackerRegistry
import kr.toxicity.model.util.PLUGIN
import kr.toxicity.model.util.registerListener
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.*
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.world.EntitiesLoadEvent
import org.bukkit.event.world.EntitiesUnloadEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.potion.PotionEffectType

object EntityManagerImpl : EntityManager, GlobalManagerImpl {

    private val effectMap = ReferenceOpenHashSet<PotionEffectType>().apply {
        add(PotionEffectType.GLOWING)
        add(PotionEffectType.INVISIBILITY)
    }

    private class PaperListener : Listener { //More accurate world change event for Paper
        @EventHandler(priority = EventPriority.MONITOR)
        fun EntityRemoveFromWorldEvent.remove() {
            EntityTrackerRegistry.registry(entity.uniqueId)?.despawn()
        }
        @EventHandler(priority = EventPriority.MONITOR)
        fun EntityAddToWorldEvent.add() {
            EntityTrackerRegistry.registry(entity)?.refresh()
        }
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        fun EntityJumpEvent.jump() {
            entity.forEachTracker { it.animate("jump") }
        }
    }

    private class SpigotListener : Listener { //Portal event for Spigot
        @EventHandler(priority = EventPriority.MONITOR)
        fun EntityRemoveEvent.remove() {
            EntityTrackerRegistry.registry(entity.uniqueId)?.despawn()
        }
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        fun EntitySpawnEvent.spawn() {
            EntityTrackerRegistry.registry(entity)?.refresh()
        }
        @EventHandler(priority = EventPriority.MONITOR)
        fun PlayerChangedWorldEvent.change() {
            EntityTrackerRegistry.registry(player.uniqueId)?.let {
                it.despawn()
                it.refresh()
            }
        }
    }

    //Event handlers
    private val standardListener = object : Listener {
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        fun EntityPotionEffectEvent.potion() { //Apply potion effect
            if (action == EntityPotionEffectEvent.Action.CHANGED) return
            if (oldEffect?.let { effectMap.contains(it.type) } == true || newEffect?.let { effectMap.contains(it.type) } == true) entity.forEachTracker { it.updateBaseEntity() }
        }
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        fun EntityDismountEvent.dismount() { //Dismount
            val e = dismounted
            isCancelled = e is HitBox && (e.mountController().canFly() || !e.mountController().canDismountBySelf()) && !e.forceDismount()
        }
        @EventHandler(priority = EventPriority.MONITOR)
        fun PlayerQuitEvent.quit() { //Quit
            EntityTrackerRegistry.registry(player.uniqueId)?.close()
            PLUGIN.scheduler().asyncTask {
                EntityTrackerRegistry.registries { registry -> registry.remove(player) }
            }
            (player.vehicle as? HitBox)?.dismount(player)
        }
        @EventHandler(priority = EventPriority.MONITOR)
        fun EntitiesLoadEvent.load() { //Chunk load
            entities.forEach { entity ->
                EntityTrackerRegistry.registry(entity.uniqueId)?.refresh()
            }
        }
        @EventHandler(priority = EventPriority.MONITOR)
        fun EntitiesUnloadEvent.unload() { //Chunk unload
            entities.forEach { entity ->
                EntityTrackerRegistry.registry(entity.uniqueId)?.despawn()
            }
        }
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        fun EntityDeathEvent.death() { //Death
            entity.forEachTracker {
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
            entity.forEachTracker {
                it.animate("damage", AnimationModifier.DEFAULT_WITH_PLAY_ONCE)
                it.damageTint()
            }
        }
    }
    private val platformListener = if (BetterModel.IS_PAPER) PaperListener() else SpigotListener()

    //Lifecycles
    override fun start() {
        registerListener(standardListener)
        registerListener(platformListener)
    }

    override fun reload(info: ReloadInfo, zipper: PackZipper) {
        EntityTrackerRegistry.registries(EntityTrackerRegistry::reload)
    }

    override fun end() {
        EntityTrackerRegistry.registries(EntityTrackerRegistry::save)
    }

    //Extension
    private fun Entity.forEachTracker(block: (EntityTracker) -> Unit) {
        EntityTrackerRegistry.registry(uniqueId)?.trackers()?.forEach(block)
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
}