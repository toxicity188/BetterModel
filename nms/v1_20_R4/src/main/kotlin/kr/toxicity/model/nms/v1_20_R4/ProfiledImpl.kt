/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.nms.v1_20_R4

import com.mojang.authlib.GameProfile
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.armor.PlayerArmor
import kr.toxicity.model.api.nms.Profiled


class ProfiledImpl(
    private val playerArmor: PlayerArmor,
    private val gameProfile: () -> GameProfile
) : Profiled {

    override fun profile(): GameProfile = gameProfile()
    override fun isSlim(): Boolean = BetterModel.plugin().skinManager().isSlim(profile())
    override fun armors(): PlayerArmor = playerArmor
}