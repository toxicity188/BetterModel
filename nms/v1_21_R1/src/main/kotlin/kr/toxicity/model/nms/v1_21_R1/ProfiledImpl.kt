/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.nms.v1_21_R1

import kr.toxicity.model.api.armor.PlayerArmor
import kr.toxicity.model.api.nms.Profiled
import kr.toxicity.model.api.player.PlayerSkinParts
import kr.toxicity.model.api.profile.ModelProfile


internal class ProfiledImpl(
    private val playerArmor: PlayerArmor,
    private val modelProfile: () -> ModelProfile,
    private val playerSkinParts: () -> PlayerSkinParts
) : Profiled {

    override fun profile(): ModelProfile = modelProfile()
    override fun armors(): PlayerArmor = playerArmor
    override fun skinParts(): PlayerSkinParts = playerSkinParts()
}
