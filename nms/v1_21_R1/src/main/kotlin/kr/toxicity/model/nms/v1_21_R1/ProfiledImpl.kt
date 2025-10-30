/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.nms.v1_21_R1

import com.mojang.authlib.GameProfile
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.nms.Profiled

class ProfiledImpl(
    private val gameProfile: GameProfile,
) : Profiled {
    private val slim by lazy {
        BetterModel.plugin().skinManager().isSlim(gameProfile)
    }

    override fun profile(): GameProfile = gameProfile
    override fun isSlim(): Boolean = slim
}