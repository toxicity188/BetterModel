/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.authlib

import com.mojang.authlib.GameProfile
import kr.toxicity.model.api.skin.AuthLibAdapter
import kr.toxicity.model.api.skin.SkinProfile

class V6AuthLibAdapter : AuthLibAdapter {
    override fun adapt(profile: GameProfile): SkinProfile {
        return SkinProfile(
            profile.id,
            profile.name,
            profile.properties["textures"]
        )
    }
}