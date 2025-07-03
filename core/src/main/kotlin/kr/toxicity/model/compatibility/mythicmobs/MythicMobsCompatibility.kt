package kr.toxicity.model.compatibility.mythicmobs

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.skills.SkillCaster
import io.lumine.mythic.bukkit.BukkitAdapter
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.bukkit.events.MythicConditionLoadEvent
import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent
import io.lumine.mythic.bukkit.events.MythicTargeterLoadEvent
import io.lumine.mythic.core.skills.SkillMetadataImpl
import io.lumine.mythic.core.skills.SkillTriggers
import kr.toxicity.model.api.script.EntityScript
import kr.toxicity.model.compatibility.Compatibility
import kr.toxicity.model.compatibility.mythicmobs.condition.ModelHasPassengerCondition
import kr.toxicity.model.compatibility.mythicmobs.mechanic.*
import kr.toxicity.model.compatibility.mythicmobs.targeter.ModelPartTargeter
import kr.toxicity.model.manager.ScriptManagerImpl
import kr.toxicity.model.util.registerListener
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class MythicMobsCompatibility : Compatibility {
    override fun start() {
        ScriptManagerImpl.addBuilder("mm") { name ->
            EntityScript { entity ->
                MythicBukkit.inst().skillManager.getSkill(name).ifPresent { skill ->
                    skill.execute(SkillMetadataImpl(
                        SkillTriggers.API,
                        object : SkillCaster {
                            private val lazyEntity by lazy {
                                BukkitAdapter.adapt(entity)
                            }
                            override fun getEntity(): AbstractEntity = lazyEntity
                            override fun setUsingDamageSkill(p0: Boolean) {
                            }
                            override fun isUsingDamageSkill(): Boolean = false
                        },
                        null
                    ))
                }
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