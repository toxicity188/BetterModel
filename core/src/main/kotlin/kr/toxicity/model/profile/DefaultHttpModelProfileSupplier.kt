/*
 * This source file is part of BetterModel.
 * Copyright (c) 2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.profile

import kr.toxicity.model.api.platform.PlatformPlayer
import kr.toxicity.model.api.profile.ModelProfile
import kr.toxicity.model.api.profile.ModelProfileInfo
import kr.toxicity.model.api.profile.ModelProfileSupplier
import kr.toxicity.model.util.PLATFORM

class DefaultHttpModelProfileSupplier : ModelProfileSupplier {

    private val http = HttpModelProfileSupplier()

    override fun supply(info: ModelProfileInfo): ModelProfile.Uncompleted {
        val player = PLATFORM.adapter().offlinePlayer(info.id)
        return if (player is PlatformPlayer) ModelProfile.of(player).asUncompleted() else http.supply(info)
    }
}
