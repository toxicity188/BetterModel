/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.spigot

import kr.toxicity.model.bukkit.BetterModelPlugin
import kr.toxicity.model.api.bukkit.BetterModelBukkit
import kr.toxicity.model.util.toComponent
import kr.toxicity.model.util.warn
import org.bukkit.Bukkit

@Suppress("UNUSED")
class BetterModelPluginSpigot : BetterModelPlugin() {

    override fun onEnable() {
        if (BetterModelBukkit.IS_PAPER) {
            warn(
                "You're using Paper, so you have to use Paper jar!".toComponent(),
                "Please download Paper jar from Modrinth! (https://modrinth.com/plugin/bettermodel)".toComponent()
            )
            return Bukkit.getPluginManager().disablePlugin(this)
        }
        super.onEnable()
    }
}
