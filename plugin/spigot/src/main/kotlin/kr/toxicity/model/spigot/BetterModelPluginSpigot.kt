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

@Suppress("UNUSED")
class BetterModelPluginSpigot : BetterModelPluginImpl() {
    override fun onLoad() {
        super.onLoad()
        CommandAPI.onLoad(CommandAPISpigotConfig(this).fallbackToLatestNMS(true).silentLogs(true))
    }
}