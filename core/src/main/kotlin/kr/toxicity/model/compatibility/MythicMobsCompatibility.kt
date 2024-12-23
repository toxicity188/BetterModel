package kr.toxicity.model.compatibility

import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent
import kr.toxicity.model.compatibility.mythicmobs.DefaultStateMechanic
import kr.toxicity.model.compatibility.mythicmobs.ModelMechanic
import kr.toxicity.model.compatibility.mythicmobs.PartVisibilityMechanic
import kr.toxicity.model.compatibility.mythicmobs.StateMechanic
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
                }
            }
        })
    }
}