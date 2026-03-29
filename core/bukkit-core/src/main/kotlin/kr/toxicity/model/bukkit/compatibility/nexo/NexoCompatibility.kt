/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.bukkit.compatibility.nexo

import com.nexomc.nexo.api.events.resourcepack.NexoPrePackGenerateEvent
import kr.toxicity.model.api.BetterModelPlatform
import kr.toxicity.model.bukkit.util.registerListener
import kr.toxicity.model.bukkit.compatibility.Compatibility
import kr.toxicity.model.bukkit.util.PLUGIN
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
                when (val result = PLATFORM.reload()) {
                    is BetterModelPlatform.ReloadResult.Success -> {
                        result.packResult().directory()?.let {
                            addResourcePack(it)
                            info("Successfully merged with Nexo.".toComponent(NamedTextColor.GREEN))
                        }
                    }

                    is BetterModelPlatform.ReloadResult.OnReload -> {
                        warn("BetterModel is still on reload!".toComponent(NamedTextColor.RED))
                    }

                    is BetterModelPlatform.ReloadResult.Failure -> {
                        result.throwable.handleException("Unable to merge with Nexo.")
                    }
                }
            }
        })
    }
}
