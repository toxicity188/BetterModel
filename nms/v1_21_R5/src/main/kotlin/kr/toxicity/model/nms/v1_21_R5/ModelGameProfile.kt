/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.nms.v1_21_R5

import com.mojang.authlib.GameProfile
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.profile.ModelProfile
import kr.toxicity.model.api.profile.ModelProfileInfo
import kr.toxicity.model.api.profile.ModelProfileSkin

internal data class ModelGameProfile(
    private val gameProfile: GameProfile
) : ModelProfile {

    private val info = ModelProfileInfo(gameProfile.id, gameProfile.name)
    private val skin by lazy {
        gameProfile.properties["textures"].firstOrNull()?.let {
            BetterModel.plugin().profileManager().skin(it.value)
        } ?: ModelProfileSkin.EMPTY
    }

    override fun info(): ModelProfileInfo = info

    override fun skin(): ModelProfileSkin = skin
}
