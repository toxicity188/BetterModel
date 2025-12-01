/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.profile

import kr.toxicity.model.api.profile.ModelProfile
import kr.toxicity.model.api.profile.ModelProfileInfo
import kr.toxicity.model.api.profile.ModelProfileSupplier
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class DefaultHttpModelProfileSupplier : ModelProfileSupplier {

    private val http = HttpModelProfileSupplier()

    override fun supply(info: ModelProfileInfo): ModelProfile.Uncompleted {
        val player = Bukkit.getOfflinePlayer(info.id)
        return if (player is Player) ModelProfile.of(player).asUncompleted() else http.supply(info)
    }
}
