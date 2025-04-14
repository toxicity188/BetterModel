package kr.toxicity.model.compatibility.mythicmobs

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.skills.SkillCaster
import io.lumine.mythic.bukkit.BukkitAdapter
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent
import io.lumine.mythic.bukkit.events.MythicTargeterLoadEvent
import io.lumine.mythic.core.skills.SkillMetadataImpl
import io.lumine.mythic.core.skills.SkillTriggers
import kr.toxicity.model.api.script.EntityScript
import kr.toxicity.model.compatibility.Compatibility
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
                            override fun getEntity(): AbstractEntity = BukkitAdapter.adapt(entity)
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
            fun load(e: MythicMechanicLoadEvent) {
                when (e.mechanicName.lowercase()) {
                    "model" -> e.register(ModelMechanic(e.config))
                    "state", "animation" -> e.register(StateMechanic(e.config))
                    "defaultstate", "defaultanimation" -> e.register(DefaultStateMechanic(e.config))
                    "partvisibility", "partvis" -> e.register(PartVisibilityMechanic(e.config))
                    "bindhitbox" -> e.register(BindHitBoxMechanic(e.config))
                    "changepart" -> e.register(ChangePartMechanic(e.config))
                    "vfx" -> e.register(VFXMechanic(e.config))
                    "tint", "color" -> e.register(TintMechanic(e.config))
                    "brightness", "light" -> e.register(BrightnessMechanic(e.config))
                    "enchant" -> e.register(EnchantMechanic(e.config))
                    "glow", "glowbone" -> e.register(GlowMechanic(e.config))
                    "mountmodel" -> e.register(MountModelMechanic(e.config))
                }
            }
            @EventHandler
            fun load(e: MythicTargeterLoadEvent) {
                when (e.targeterName.lowercase()) {
                    "modelpart" -> e.register(ModelPartTargeter(e.config))
                }
            }
        })
    }
}