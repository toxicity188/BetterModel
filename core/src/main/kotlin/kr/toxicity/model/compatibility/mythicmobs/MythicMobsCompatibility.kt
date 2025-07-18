package kr.toxicity.model.compatibility.mythicmobs

import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.bukkit.events.MythicConditionLoadEvent
import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent
import io.lumine.mythic.bukkit.events.MythicTargeterLoadEvent
import kr.toxicity.model.api.data.renderer.RenderSource
import kr.toxicity.model.api.script.AnimationScript
import kr.toxicity.model.compatibility.Compatibility
import kr.toxicity.model.compatibility.mythicmobs.condition.ModelHasPassengerCondition
import kr.toxicity.model.compatibility.mythicmobs.mechanic.*
import kr.toxicity.model.compatibility.mythicmobs.targeter.ModelPartTargeter
import kr.toxicity.model.manager.ScriptManagerImpl
import kr.toxicity.model.util.registerListener
import kr.toxicity.model.util.warn
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class MythicMobsCompatibility : Compatibility {
    override fun start() {
        ScriptManagerImpl.addBuilder("mm") { name ->
            AnimationScript.of script@ { source ->
                if (source !is RenderSource.Entity) return@script
                if (!MythicBukkit.inst().apiHelper.castSkill(
                    source.entity(),
                    name,
                    MythicBukkit.inst().apiHelper.getMythicMobInstance(source.entity())?.power ?: 1F
                )) warn("Unknown MythicMobs skill name: $name")
            }
        }
        registerListener(object : Listener {
            @EventHandler
            fun MythicMechanicLoadEvent.load() {
                when (mechanicName.lowercase()) {
                    "playlimbanim" -> register(PlayLimbAnimMechanic(config))
                    "model" -> register(ModelMechanic(config))
                    "state", "animation" -> register(StateMechanic(config))
                    "defaultstate", "defaultanimation" -> register(DefaultStateMechanic(config))
                    "partvisibility", "partvis" -> register(PartVisibilityMechanic(config))
                    "bindhitbox" -> register(BindHitBoxMechanic(config))
                    "changepart" -> register(ChangePartMechanic(config))
                    "vfx" -> register(VFXMechanic(config))
                    "tint", "color" -> register(TintMechanic(config))
                    "brightness", "light" -> register(BrightnessMechanic(config))
                    "enchant" -> register(EnchantMechanic(config))
                    "glow", "glowbone" -> register(GlowMechanic(config))
                    "mountmodel" -> register(MountModelMechanic(config))
                    "dismountmodel" -> register(DismountModelMechanic(config))
                    "dismountallmodel", "dismountall" -> register(DismountAllModelMechanic(config))
                    "lockmodel", "lockrotation" -> register(LockModelMechanic(config))
                }
            }
            @EventHandler
            fun MythicConditionLoadEvent.load() {
                when (conditionName.lowercase()) {
                    "modelhaspassenger" -> register(ModelHasPassengerCondition(config))
                }
            }
            @EventHandler
            fun MythicTargeterLoadEvent.load() {
                when (targeterName.lowercase()) {
                    "modelpart" -> register(ModelPartTargeter(config))
                }
            }
        })
    }
}