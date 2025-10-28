/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.spigot

import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPISpigotConfig
import kr.toxicity.model.BetterModelPluginImpl
import kr.toxicity.model.util.toComponent
import kr.toxicity.model.util.warn
import org.bukkit.Bukkit

@Suppress("UNUSED")
class BetterModelPluginSpigot : BetterModelPluginImpl() {

    private var shouldBeDisabled = false

    override fun onLoad() {
        super.onLoad()
        try {
            CommandAPI.onLoad(CommandAPISpigotConfig(this).fallbackToLatestNMS(true).silentLogs(true))
        } catch (_: NoClassDefFoundError) {
            shouldBeDisabled = true
        }
    }

    override fun onEnable() {
        if (shouldBeDisabled) {
            warn(
                "You're using Paper, so you have to use Paper jer!".toComponent(),
                "Please download Paper jar from Modrinth! (https://modrinth.com/plugin/bettermodel)".toComponent()
            )
            return Bukkit.getPluginManager().disablePlugin(this)
        }
        super.onEnable()
    }
}