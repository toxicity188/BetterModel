package kr.toxicity.model.manager

import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.manager.CompatibilityManager
import kr.toxicity.model.api.manager.ReloadInfo
import kr.toxicity.model.api.pack.PackZipper
import kr.toxicity.model.compatibility.citizens.CitizensCompatibility
import kr.toxicity.model.compatibility.hmccosmetics.HMCCosmeticsCompatibility
import kr.toxicity.model.compatibility.mythicmobs.MythicMobsCompatibility
import kr.toxicity.model.compatibility.skinsrestorer.SkinsRestorerCompatibility
import kr.toxicity.model.purpur.PurpurHook
import kr.toxicity.model.util.info
import org.bukkit.Bukkit

object CompatibilityManagerImpl : CompatibilityManager, GlobalManagerImpl {

    private val compatibilities = mapOf(
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
            compatibilities.forEach { (k, v) ->
                if (isPluginEnabled(k)) {
                    v().start()
                    info("Plugin hooks $k")
                }
            }
        }
    }

    override fun reload(info: ReloadInfo, zipper: PackZipper) {
    }
}