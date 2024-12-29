package kr.toxicity.model.manager

import kr.toxicity.model.api.manager.CompatibilityManager
import kr.toxicity.model.compatibility.citizens.CitizensCompatibility
import kr.toxicity.model.compatibility.mythicmobs.MythicMobsCompatibility
import kr.toxicity.model.util.info
import org.bukkit.Bukkit

object CompatibilityManagerImpl : CompatibilityManager, GlobalManagerImpl {

    private val compatibilities = mapOf(
        "MythicMobs" to {
            MythicMobsCompatibility()
        },
        "Citizens" to {
            CitizensCompatibility()
        }
    )

    override fun start() {
        Bukkit.getPluginManager().run {
            compatibilities.forEach { (k, v) ->
                if (isPluginEnabled(k)) {
                    info("Plugin hooks $k")
                    v().start()
                }
            }
        }
    }

    override fun reload() {
    }
}