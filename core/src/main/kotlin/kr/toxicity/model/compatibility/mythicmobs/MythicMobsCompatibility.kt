package kr.toxicity.model.compatibility.mythicmobs

import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent
import io.lumine.mythic.bukkit.events.MythicTargeterLoadEvent
import kr.toxicity.model.compatibility.Compatibility
import kr.toxicity.model.compatibility.mythicmobs.mechanic.*
import kr.toxicity.model.compatibility.mythicmobs.targeter.ModelPartTargeter
import kr.toxicity.model.util.registerListener
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class MythicMobsCompatibility : Compatibility {
    override fun start() {
        registerListener(object : Listener {
            @EventHandler
            fun load(e: MythicMechanicLoadEvent) {
                when (e.mechanicName.lowercase()) {
                    "model" -> e.register(ModelMechanic(e.config))
                    "state" -> e.register(StateMechanic(e.config))
                    "defaultstate" -> e.register(DefaultStateMechanic(e.config))
                    "partvisibility" -> e.register(PartVisibilityMechanic(e.config))
                    "bindhitbox" -> e.register(BindHitBoxMechanic(e.config))
                    "changepart" -> e.register(ChangePartMechanic(e.config))
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