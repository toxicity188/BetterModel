package kr.toxicity.model.manager

import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.manager.CompatibilityManager
import kr.toxicity.model.api.pack.PackZipper
import kr.toxicity.model.compatibility.citizens.CitizensCompatibility
import kr.toxicity.model.compatibility.hmccosmetics.HMCCosmeticsCompatibility
import kr.toxicity.model.compatibility.mythicmobs.MythicMobsCompatibility
import kr.toxicity.model.compatibility.skinsrestorer.SkinsRestorerCompatibility
import kr.toxicity.model.purpur.PurpurHook
import kr.toxicity.model.util.info
import kr.toxicity.model.util.registerListener
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.PluginEnableEvent

object CompatibilityManagerImpl : CompatibilityManager, GlobalManager {

    private val compatibilities = mutableMapOf(
        "MythicMobs" to {
            MythicMobsCompatibility()
        },
        "Citizens" to {
            CitizensCompatibility()
        },
        "HMCCosmetics" to {
            HMCCosmeticsCompatibility()
        },
        "SkinsRestorer" to {
            SkinsRestorerCompatibility()
        }
    )

    override fun start() {
        if (BetterModel.IS_PURPUR) PurpurHook.start()
        Bukkit.getPluginManager().run {
            compatibilities.entries.removeIf { (k, v) ->
                if (isPluginEnabled(k)) {
                    v().start()
                    info("Plugin hooks $k")
                    true
                } else false
            }
        }
        registerListener(object : Listener {
            @EventHandler
            fun PluginEnableEvent.enable() {
                val name = plugin.name
                compatibilities.remove(name)?.let {
                    it().start()
                    info("Plugin hooks $name")
                }
            }
        })
    }

    override fun reload(pipeline: ReloadPipeline, zipper: PackZipper) {
    }
}