/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.compatibility.nexo

import com.nexomc.nexo.api.events.resourcepack.NexoPrePackGenerateEvent
import kr.toxicity.model.api.BetterModelPlugin
import kr.toxicity.model.compatibility.Compatibility
import kr.toxicity.model.util.*
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class NexoCompatibility : Compatibility {
    override fun start() {
        if (CONFIG.mergeWithExternalResources()) PLUGIN.skipInitialReload()
        registerListener(object : Listener {
            @EventHandler
            fun NexoPrePackGenerateEvent.generate() {
                if (!CONFIG.mergeWithExternalResources()) return
                when (val result = PLUGIN.reload()) {
                    is BetterModelPlugin.ReloadResult.Success -> {
                        result.packResult().directory()?.let {
                            addResourcePack(it)
                            info("Successfully merged with Nexo.".toComponent(NamedTextColor.GREEN))
                        }
                    }
                    is BetterModelPlugin.ReloadResult.OnReload -> {
                        warn("BetterModel is still on reload!".toComponent(NamedTextColor.RED))
                    }
                    is BetterModelPlugin.ReloadResult.Failure -> {
                        result.throwable.handleException("Unable to merge with Nexo.")
                    }
                }
            }
        })
    }
}