/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.bukkit.manager

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent
import com.destroystokyo.paper.event.entity.EntityJumpEvent
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import it.unimi.dsi.fastutil.objects.ReferenceSet
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.animation.AnimationModifier
import kr.toxicity.model.api.bukkit.BetterModelBukkit
import kr.toxicity.model.api.nms.HitBox
import kr.toxicity.model.api.nms.ModelInteractionHand
import kr.toxicity.model.api.pack.PackZipper
import kr.toxicity.model.api.tracker.EntityTracker
import kr.toxicity.model.api.tracker.EntityTrackerRegistry
import kr.toxicity.model.api.tracker.Tracker
import kr.toxicity.model.bukkit.util.registerListener
import kr.toxicity.model.manager.GlobalManager
import kr.toxicity.model.manager.ReloadPipeline
import kr.toxicity.model.bukkit.nms.v1_21_R4.wrap
import kr.toxicity.model.util.PLATFORM
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityDismountEvent
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.event.entity.EntityRemoveEvent
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.world.EntitiesUnloadEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.potion.PotionEffectType
import org.joml.Vector3f

object EntityManager : GlobalManager {

    private val effectSet = ReferenceSet.of(
        PotionEffectType.GLOWING,
        PotionEffectType.INVISIBILITY
    )

    private class PaperListener : Listener { //More accurate world change event for Paper
        @EventHandler(priority = EventPriority.MONITOR)
        fun EntityRemoveFromWorldEvent.remove() {
            BetterModel.registryOrNull(entity.uniqueId)?.despawn()
        }
        @EventHandler(priority = EventPriority.MONITOR)
        fun EntityAddToWorldEvent.add() {
            BetterModel.registryOrNull(entity.wrap())?.refresh()
        }
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        fun EntityJumpEvent.jump() {
            entity.forEachTracker { it.animate("jump") }
        }
    }

    private class SpigotListener : Listener { //Portal event for Spigot
        @EventHandler(priority = EventPriority.MONITOR)
        fun EntityRemoveEvent.remove() {
            BetterModel.registryOrNull(entity.uniqueId)?.despawn()
        }
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        fun EntitySpawnEvent.spawn() {
            BetterModel.registryOrNull(entity.wrap())?.refresh()
        }
        @EventHandler(priority = EventPriority.MONITOR)
        fun PlayerChangedWorldEvent.change() {
            BetterModel.registryOrNull(player.uniqueId)?.let {
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
            if (oldEffect?.let { it.type in effectSet } == true || newEffect?.let { it.type in effectSet } == true) entity.forEachTracker { it.updateBaseEntity() }
        }
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        fun EntityDismountEvent.dismount() { //Dismount
            val e = dismounted
            isCancelled = e is HitBox && (e.mountController().canFly() || !e.mountController().canDismountBySelf()) && !e.forceDismount()
        }
        @EventHandler(priority = EventPriority.MONITOR)
        fun PlayerQuitEvent.quit() { //Quit
            val wrap = player.wrap()
            BetterModel.registryOrNull(wrap.uuid())?.close()
            PLATFORM.scheduler().asyncTask {
                EntityTrackerRegistry.registries { registry -> registry.remove(wrap) }
            }
            (player.vehicle as? HitBox)?.dismount(wrap)
        }
        @EventHandler(priority = EventPriority.MONITOR)
        fun PlayerDeathEvent.death() {
            BetterModel.registryOrNull(entity.uniqueId)?.despawn()
        }
        @EventHandler(priority = EventPriority.MONITOR)
        fun EntitiesUnloadEvent.unload() { //Chunk unload
            entities.forEach { entity ->
                BetterModel.registryOrNull(entity.uniqueId)?.despawn()
            }
        }
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        fun EntityDeathEvent.death() { //Death
            entity.forEachTracker {
                if (it.animate("death", AnimationModifier.DEFAULT_WITH_PLAY_ONCE) { it.close() }) {
                    it.forRemoval(true)
                }
            }
        }
        @EventHandler(priority = EventPriority.MONITOR)
        fun PlayerInteractAtEntityEvent.interact() {
            val pos = clickedPosition
            (rightClicked as? HitBox)?.triggerInteractAt(
                player.wrap(),
                when (this.hand) {
                    EquipmentSlot.HAND -> ModelInteractionHand.RIGHT
                    EquipmentSlot.OFF_HAND -> ModelInteractionHand.LEFT
                    else -> return
                },
                Vector3f(
                    pos.x.toFloat(),
                    pos.y.toFloat(),
                    pos.z.toFloat()
                )
            )
        }

        @EventHandler(priority = EventPriority.MONITOR)
        fun PlayerInteractEntityEvent.interact() { //Interact base entity based on interaction entity
            val isRight = hand == EquipmentSlot.HAND
            val dismount = isRight && player.triggerDismount(rightClicked)
            (rightClicked as? HitBox)?.let {
                it.triggerInteract(
                    player.wrap(),
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
                    if (this is HitBox) source().uuid() else uniqueId
                }
                val v = damager.vehicle
                if (v is HitBox && !v.mountController().canBeDamagedByRider() && v.source().uuid() == victim) {
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
    private val platformListener = if (BetterModelBukkit.IS_PAPER) PaperListener() else SpigotListener()

    //Lifecycles
    override fun start() {
        registerListener(standardListener)
        registerListener(platformListener)
    }

    override fun reload(pipeline: ReloadPipeline, zipper: PackZipper) {
        EntityTrackerRegistry.registries(EntityTrackerRegistry::reload)
    }

    override fun end() {
        EntityTrackerRegistry.registries {
            it.save()
            it.close(Tracker.CloseReason.PLUGIN_DISABLE)
        }
    }

    //Extension
    private fun Entity.forEachTracker(block: (EntityTracker) -> Unit) {
        BetterModel.registryOrNull(uniqueId)?.trackers()?.forEach(block)
    }

    private fun Player.triggerDismount(e: Entity): Boolean {
        val previous = vehicle
        if (previous !is HitBox) return false
        val uuid = if (e is HitBox) e.source().uuid() else e.uniqueId
        if (previous.source().uuid() == uuid && previous.mountController().canDismountBySelf()) {
            previous.dismount(wrap())
            return true
        }
        return false
    }

    private fun Player.triggerMount(hitBox: HitBox) {
        if (hitBox.mountController().canMount()) hitBox.mount(wrap())
    }
}
