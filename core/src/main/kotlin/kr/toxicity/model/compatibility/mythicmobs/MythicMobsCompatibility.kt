/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.compatibility.mythicmobs

import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.bukkit.events.MythicConditionLoadEvent
import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent
import io.lumine.mythic.bukkit.events.MythicTargeterLoadEvent
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.entity.BaseBukkitEntity
import kr.toxicity.model.api.script.AnimationScript
import kr.toxicity.model.api.tracker.EntityTracker
import kr.toxicity.model.compatibility.Compatibility
import kr.toxicity.model.compatibility.mythicmobs.condition.ModelHasPassengerCondition
import kr.toxicity.model.compatibility.mythicmobs.mechanic.*
import kr.toxicity.model.compatibility.mythicmobs.targeter.ModelPartTargeter
import kr.toxicity.model.manager.ScriptManagerImpl
import kr.toxicity.model.util.CONFIG
import kr.toxicity.model.util.componentOf
import kr.toxicity.model.util.registerListener
import kr.toxicity.model.util.toComponent
import kr.toxicity.model.util.warn
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class MythicMobsCompatibility : Compatibility {

    private companion object {
        const val NAMESPACE = "bm:"
    }

    override fun start() {
        ScriptManagerImpl.addBuilder("mm") { name ->
            val args = name.args() ?: return@addBuilder AnimationScript.EMPTY
            AnimationScript.of(BetterModel.IS_FOLIA) script@ { tracker ->
                if (!CONFIG.module().model) return@script
                if (tracker !is EntityTracker) return@script
                val entity = (tracker.registry().entity() as? BaseBukkitEntity ?: return@script).entity()
                if (!MythicBukkit.inst().apiHelper.castSkill(
                        entity,
                        args,
                    MythicBukkit.inst().apiHelper.getMythicMobInstance(entity)?.power ?: 1F
                ) {
                    name.metadata.toMap().forEach { (key, value) ->
                        it.parameters[key] = value.toString()
                    }
                }) warn(componentOf(
                    "Unknown MythicMobs skill name: ".toComponent(),
                    args.toComponent(NamedTextColor.RED)
                ))
            }
        }
        registerListener(object : Listener {
            @EventHandler
            fun MythicMechanicLoadEvent.load() {
                if (!CONFIG.module().model) return
                when (mechanicName.lowercase().substringAfter(NAMESPACE)) {
                    "playlimbanim" -> register(PlayLimbAnimMechanic(config))
                    "model" -> register(ModelMechanic(config))
                    "state", "animation" -> register(StateMechanic(config))
                    "defaultstate", "defaultanimation" -> register(DefaultStateMechanic(config))
                    "partvisibility", "partvis" -> register(PartVisibilityMechanic(config))
                    "bindhitbox" -> register(BindHitBoxMechanic(config))
                    "changepart" -> register(ChangePartMechanic(config))
                    "tint", "color" -> register(TintMechanic(config))
                    "brightness", "light" -> register(BrightnessMechanic(config))
                    "enchant" -> register(EnchantMechanic(config))
                    "billboard" -> register(BillboardMechanic(config))
                    "glow", "glowbone" -> register(GlowMechanic(config))
                    "mountmodel" -> register(MountModelMechanic(config))
                    "dismountmodel" -> register(DismountModelMechanic(config))
                    "dismountallmodel", "dismountall" -> register(DismountAllModelMechanic(config))
                    "lockmodel", "lockrotation" -> register(LockModelMechanic(config))
                    "bodyrotation", "bodyclamp" -> register(BodyRotationMechanic(config))
                    "remapmodel", "remap" -> register(RemapModelMechanic(config))
                    "pairmodel" -> register(PairModelMechanic(config))
                }
            }
            @EventHandler
            fun MythicConditionLoadEvent.load() {
                if (!CONFIG.module().model) return
                when (conditionName.lowercase().substringAfter(NAMESPACE)) {
                    "modelhaspassenger" -> register(ModelHasPassengerCondition(config))
                }
            }
            @EventHandler
            fun MythicTargeterLoadEvent.load() {
                if (!CONFIG.module().model) return
                when (targeterName.lowercase().substringAfter(NAMESPACE)) {
                    "modelpart" -> register(ModelPartTargeter(config))
                }
            }
        })
    }
}