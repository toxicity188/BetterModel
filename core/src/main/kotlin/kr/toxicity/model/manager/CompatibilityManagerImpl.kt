/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.manager

import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.manager.CompatibilityManager
import kr.toxicity.model.api.pack.PackZipper
import kr.toxicity.model.compatibility.citizens.CitizensCompatibility
import kr.toxicity.model.compatibility.hmccosmetics.HMCCosmeticsCompatibility
import kr.toxicity.model.compatibility.mythicmobs.MythicMobsCompatibility
import kr.toxicity.model.compatibility.nexo.NexoCompatibility
import kr.toxicity.model.compatibility.skinsrestorer.SkinsRestorerCompatibility
import kr.toxicity.model.purpur.PurpurHook
import kr.toxicity.model.util.info
import kr.toxicity.model.util.registerListener
import kr.toxicity.model.util.toComponent
import net.kyori.adventure.text.format.NamedTextColor
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
        },
        "Nexo" to {
            NexoCompatibility()
        }
    )

    override fun start() {
        if (BetterModel.IS_PURPUR) PurpurHook.start()
        Bukkit.getPluginManager().run {
            compatibilities.entries.removeIf { (k, v) ->
                if (isPluginEnabled(k)) {
                    v().start()
                    k.hookMessage()
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
                    name.hookMessage()
                }
            }
        })
    }

    private fun String.hookMessage() = info("Plugin hooks $this".toComponent(NamedTextColor.AQUA))

    override fun reload(pipeline: ReloadPipeline, zipper: PackZipper) {
    }
}