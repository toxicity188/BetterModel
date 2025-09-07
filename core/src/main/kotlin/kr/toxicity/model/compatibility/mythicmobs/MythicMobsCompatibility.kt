package kr.toxicity.model.compatibility.mythicmobs

import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.bukkit.events.MythicConditionLoadEvent
import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent
import io.lumine.mythic.bukkit.events.MythicTargeterLoadEvent
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.script.AnimationScript
import kr.toxicity.model.api.tracker.EntityTracker
import kr.toxicity.model.compatibility.Compatibility
import kr.toxicity.model.compatibility.mythicmobs.condition.ModelHasPassengerCondition
import kr.toxicity.model.compatibility.mythicmobs.mechanic.*
import kr.toxicity.model.compatibility.mythicmobs.targeter.ModelPartTargeter
import kr.toxicity.model.manager.ScriptManagerImpl
import kr.toxicity.model.util.CONFIG
import kr.toxicity.model.util.registerListener
import kr.toxicity.model.util.warn
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class MythicMobsCompatibility : Compatibility {
    override fun start() {
        ScriptManagerImpl.addBuilder("mm") { name ->
            val args = name.args() ?: return@addBuilder AnimationScript.EMPTY
            AnimationScript.of(BetterModel.IS_FOLIA) script@ { tracker ->
                if (!CONFIG.module().model) return@script
                if (tracker !is EntityTracker) return@script
                if (!MythicBukkit.inst().apiHelper.castSkill(
                        tracker.registry().entity(),
                        args,
                    MythicBukkit.inst().apiHelper.getMythicMobInstance(tracker.registry().entity())?.power ?: 1F
                ) {
                    name.metadata.toMap().forEach { (key, value) ->
                        it.parameters[key] = value.toString()
                    }
                }) warn("Unknown MythicMobs skill name: $args")
            }
        }
        registerListener(object : Listener {
            @EventHandler
            fun MythicMechanicLoadEvent.load() {
                if (!CONFIG.module().model) return
                when (mechanicName.lowercase()) {
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
                }
            }
            @EventHandler
            fun MythicConditionLoadEvent.load() {
                if (!CONFIG.module().model) return
                when (conditionName.lowercase()) {
                    "modelhaspassenger" -> register(ModelHasPassengerCondition(config))
                }
            }
            @EventHandler
            fun MythicTargeterLoadEvent.load() {
                if (!CONFIG.module().model) return
                when (targeterName.lowercase()) {
                    "modelpart" -> register(ModelPartTargeter(config))
                }
            }
        })
    }
}