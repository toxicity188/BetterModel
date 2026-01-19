/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.fabric.manager

import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.animation.AnimationModifier
import kr.toxicity.model.api.fabric.entity.BaseFabricEntity
import kr.toxicity.model.api.nms.HitBox
import kr.toxicity.model.api.nms.ModelInteractionHand
import kr.toxicity.model.api.pack.PackZipper
import kr.toxicity.model.api.tracker.EntityTracker
import kr.toxicity.model.api.tracker.EntityTrackerRegistry
import kr.toxicity.model.api.tracker.Tracker
import kr.toxicity.model.fabric.events.ServerEntityDismountCallback
import kr.toxicity.model.fabric.events.ServerLivingEntityJumpCallback
import kr.toxicity.model.fabric.events.ServerMobEffectLoadCallback
import kr.toxicity.model.fabric.events.ServerMobEffectUnloadCallback
import kr.toxicity.model.fabric.wrap
import kr.toxicity.model.manager.GlobalManager
import kr.toxicity.model.manager.ReloadPipeline
import kr.toxicity.model.util.PLATFORM
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import org.joml.Vector3f

object EntityManager : GlobalManager {
    override fun reload(pipeline: ReloadPipeline, zipper: PackZipper) {
        EntityTrackerRegistry.registries { registry ->
            registry.reload()
        }
    }

    override fun end() {
        EntityTrackerRegistry.registries { registry ->
            registry.save()
            registry.close(Tracker.CloseReason.PLUGIN_DISABLE)
        }
    }

    /*
        EntityAddToWorldEvent ✅
        EntityRemoveFromWorldEvent ✅
        EntityJumpEvent ✅
        EntityDamageEvent ✅
        EntityDamageByEntityEvent ✅
        EntityDeathEvent ✅
        EntityDismountEvent ✅
        EntityPotionEffectEvent ✅
        EntityRemoveEvent ✅
        EntitySpawnEvent ✅
        PlayerChangedWorldEvent ✅
        PlayerDeathEvent ✅
        PlayerInteractAtEntityEvent ✅
        PlayerInteractEntityEvent ✅
        PlayerQuitEvent ✅
        EntitiesUnloadEvent ❌ probably because ENTITY_UNLOAD contains this
     */
    override fun start() {
        registerStateEvents()
        registerLifecycleEvents()
        registerCombatEvents()
        registerInteractionEvents()
    }

    private fun registerStateEvents() {
        // same as EntityPotionEffectEvent (added)
        ServerMobEffectLoadCallback.EVENT.register { entity, instance ->
            if (instance.effect == MobEffects.GLOWING ||
                instance.effect == MobEffects.INVISIBILITY
            ) {
                entity.eachTracker { tracker ->
                    tracker.updateBaseEntity()
                }
            }
        }

        // same as EntityPotionEffectEvent (removed)
        ServerMobEffectUnloadCallback.EVENT.register { entity, instance ->
            if (instance.effect == MobEffects.GLOWING ||
                instance.effect == MobEffects.INVISIBILITY
            ) {
                entity.eachTracker { tracker ->
                    tracker.updateBaseEntity()
                }
            }
        }

        // same as EntityDismountEvent
        ServerEntityDismountCallback.EVENT.register { _, vehicle ->
            vehicle is HitBox &&
                (vehicle.mountController().canFly() || !vehicle.mountController().canDismountBySelf()) &&
                !vehicle.forceDismount()
        }

        // same as EntityJumpEvent
        ServerLivingEntityJumpCallback.EVENT.register { entity ->
            entity.eachTracker { tracker ->
                tracker.animate("jump")
            }
        }
    }

    private fun registerLifecycleEvents() {
        ServerEntityWorldChangeEvents.AFTER_ENTITY_CHANGE_WORLD.register { oldEntity, newEntity, _, _ ->
            BetterModel.registryOrNull(oldEntity.uuid)?.let { registry ->
                (registry.entity() as BaseFabricEntity).entity(newEntity)
            }
        }

        // same as EntityAddToWorldEvent, EntitySpawnEvent
        ServerEntityEvents.ENTITY_LOAD.register { entity, _ ->
            BetterModel.registryOrNull(entity.uuid)?.refresh()
        }

        // same as EntityRemoveFromWorldEvent, EntityRemoveEvent
        ServerEntityEvents.ENTITY_UNLOAD.register { entity, _ ->
            BetterModel.registryOrNull(entity.uuid)?.despawn()
        }

        // same as PlayerChangedWorldEvent
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register { player, _, _ ->
            BetterModel.registryOrNull(player.uuid)?.let { registry ->
                registry.despawn()
                registry.refresh()
            }
        }

        // same as PlayerQuitEvent
        ServerPlayerEvents.LEAVE.register { player ->
            val fabricPlayer = player.connection.wrap()
            BetterModel.registryOrNull(fabricPlayer.uuid())?.close()

            PLATFORM.scheduler().asyncTask {
                EntityTrackerRegistry.registries { registry ->
                    registry.remove(fabricPlayer)
                }
            }

            (player.vehicle as? HitBox)?.dismount(fabricPlayer)
        }
    }

    private fun registerCombatEvents() {
        // same as EntityDamageByEntityEvent
        //
        // EntityDamageByEntityEvent are not expected to be called for non-living entities.
        // therefore, ServerLivingEntityEvents is used.
        //
        ServerLivingEntityEvents.ALLOW_DAMAGE.register { entity, source, _ ->
            val damager = source.entity
            if (damager != null) {
                val victim = if (entity is HitBox) entity.source().uuid() else entity.uuid
                val vehicle = damager.vehicle
                if (vehicle is HitBox &&
                    !vehicle.mountController().canBeDamagedByRider() &&
                    vehicle.source().uuid() == victim
                ) {
                    return@register false
                }
            }

            return@register true
        }

        // same as EntityDamageEvent
        //
        // EntityDamageEvent and EntityDamageByEntityEvent are not expected to be called for non-living entities.
        // therefore, ServerLivingEntityEvents is used.
        //
        ServerLivingEntityEvents.AFTER_DAMAGE.register { entity, _, _, _, _ ->
            entity.eachTracker { tracker ->
                tracker.animate("damage", AnimationModifier.DEFAULT_WITH_PLAY_ONCE)
                tracker.damageTint()
            }
        }

        // same as EntityDeathEvent, PlayerDeathEvent
        ServerLivingEntityEvents.AFTER_DEATH.register { entity, _ ->
            entity.eachTracker { tracker ->
                val animated = tracker.animate(
                    "death",
                    AnimationModifier.DEFAULT_WITH_PLAY_ONCE
                ) {
                    tracker.close()
                }

                if (animated) {
                    tracker.forRemoval(true)
                }
            }

            if (entity is ServerPlayer) {
                BetterModel.registryOrNull(entity.uuid)?.despawn()
            }
        }
    }

    private fun registerInteractionEvents() {
        // same as PlayerInteractAtEntityEvent, PlayerInteractEntityEvent
        UseEntityCallback.EVENT.register { clicker, _, hand, clicked, hitResult ->
            if (clicker !is ServerPlayer) {
                return@register InteractionResult.PASS
            }
            val connection = clicker.connection

            // for PlayerInteractAtEntityEvent
            hitResult?.let { hitResult ->
                (clicked as? HitBox)?.triggerInteractAt(
                    connection.wrap(),
                    when (hand) {
                        InteractionHand.MAIN_HAND -> ModelInteractionHand.RIGHT
                        InteractionHand.OFF_HAND -> ModelInteractionHand.LEFT
                    },
                    Vector3f(
                        hitResult.location.x.toFloat(),
                        hitResult.location.y.toFloat(),
                        hitResult.location.z.toFloat(),
                    )
                )
            }

            // for PlayerInteractEntityEvent
            val isMainHand = hand == InteractionHand.MAIN_HAND
            val isDismounted = isMainHand && clicker.triggerDismount(clicked)

            (clicked as? HitBox)?.let { hitBox ->
                hitBox.triggerInteract(
                    connection.wrap(),
                    when (hand) {
                        InteractionHand.MAIN_HAND -> ModelInteractionHand.RIGHT
                        InteractionHand.OFF_HAND -> ModelInteractionHand.LEFT
                    }
                )
                if (isMainHand && !isDismounted) {
                    clicker.triggerMount(hitBox)
                }
            }

            return@register InteractionResult.PASS
        }
    }

    private fun ServerPlayer.triggerDismount(entity: Entity): Boolean {
        val oldVehicle = vehicle
        if (oldVehicle !is HitBox) {
            return false
        }

        val uuid = if (entity is HitBox) entity.source().uuid() else entity.uuid
        if (oldVehicle.source().uuid() != uuid || !oldVehicle.mountController().canDismountBySelf()) {
            return false
        }

        oldVehicle.dismount(connection.wrap())
        return true
    }

    private fun ServerPlayer.triggerMount(hitBox: HitBox) {
        if (hitBox.mountController().canMount()) {
            hitBox.mount(connection.wrap())
        }
    }

    private fun Entity.eachTracker(block: (EntityTracker) -> Unit) {
        BetterModel.registryOrNull(uuid)?.trackers()?.forEach { tracker ->
            block(tracker)
        }
    }
}
