/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.bukkit.compatibility.mythicmobs


import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.bukkit.events.MythicConditionLoadEvent
import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent
import io.lumine.mythic.bukkit.events.MythicReloadedEvent
import io.lumine.mythic.bukkit.events.MythicTargeterLoadEvent
import kr.toxicity.model.api.bukkit.BetterModelBukkit
import kr.toxicity.model.api.bukkit.entity.BaseBukkitEntity
import kr.toxicity.model.api.script.AnimationScript
import kr.toxicity.model.api.tracker.EntityTracker
import kr.toxicity.model.bukkit.compatibility.Compatibility
import kr.toxicity.model.bukkit.compatibility.mythicmobs.condition.*
import kr.toxicity.model.bukkit.compatibility.mythicmobs.mechanic.*
import kr.toxicity.model.bukkit.compatibility.mythicmobs.placeholder.CasterModelYawPlaceholder
import kr.toxicity.model.bukkit.compatibility.mythicmobs.targeter.ModelDriverTargeter
import kr.toxicity.model.bukkit.compatibility.mythicmobs.targeter.ModelPartTargeter
import kr.toxicity.model.bukkit.compatibility.mythicmobs.targeter.ModelPassengersTargeter
import kr.toxicity.model.bukkit.compatibility.mythicmobs.targeter.MountedModelTargeter
import kr.toxicity.model.bukkit.util.registerListener
import kr.toxicity.model.manager.ScriptManagerImpl
import kr.toxicity.model.util.CONFIG
import kr.toxicity.model.util.componentOf
import kr.toxicity.model.util.toComponent
import kr.toxicity.model.util.warn
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class MythicMobsCompatibility : Compatibility {

    private companion object {
        const val NAMESPACE = "bm:"
    }

    override fun start() {
        fun hasModelEngine() = Bukkit.getPluginManager().getPlugin("ModelEngine") != null
        fun String.componentName(): String {
            val name = lowercase().substringAfter(NAMESPACE)
            return if (name.startsWith("meg:") && !hasModelEngine()) name.removePrefix("meg:") else name
        }

        fun registerYaw() {
            if (!CONFIG.module().model || hasModelEngine()) return
            MythicBukkit.inst().placeholderManager.register(CasterModelYawPlaceholder::class.java)
        }

        registerYaw()
        ScriptManagerImpl.addBuilder("mm") { name ->
            val args = name.args() ?: return@addBuilder AnimationScript.EMPTY
            AnimationScript.of(BetterModelBukkit.IS_FOLIA) script@ { tracker ->
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
            fun MythicReloadedEvent.reloadYaw() = registerYaw()

            @EventHandler
            fun MythicMechanicLoadEvent.load() {
                if (!CONFIG.module().model) return
                when (mechanicName.componentName()) {
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
                    "scale" -> register(ScaleMechanic(config))
                    "nofall", "cancelfall", "cancelfalldamage" -> register(NoFallMechanic(config))
                }
            }

            @EventHandler
            fun MythicConditionLoadEvent.load() {
                if (!CONFIG.module().model) return
                when (conditionName.componentName()) {
                    "modelhaspassenger" -> register(ModelHasPassengerCondition(config))
                    "modelhaspassengers", "modelpassengers", "modelpassenger" -> register(ModelHasPassengersCondition(config))
                    "hasmodel" -> register(HasModelCondition(config))
                    "partvisibility", "partvis" -> register(PartVisibilityCondition(config))
                    "hitbox", "hitboxissubhitbox", "issubhitbox" -> register(HitBoxCondition(config, argument))
                    "modelhasdriver", "modeldriver" -> register(ModelHasDriverCondition())
                    "drivingmodel" -> register(DrivingModelCondition())
                    "ridingmodel" -> register(RidingModelCondition())
                }
            }

            @EventHandler
            fun MythicTargeterLoadEvent.load() {
                if (!CONFIG.module().model) return
                when (targeterName.componentName()) {
                    "modelpart" -> register(ModelPartTargeter(config))
                    "mountedmodel" -> register(MountedModelTargeter())
                    "modelpassengers" -> register(ModelPassengersTargeter(config))
                    "modeldriver" -> register(ModelDriverTargeter())
                }
            }
        })
    }
}
