package kr.toxicity.model.authlib

import com.mojang.authlib.GameProfile
import kr.toxicity.model.api.skin.AuthLibAdapter
import kr.toxicity.model.api.skin.SkinProfile

class V7AuthLibAdapter : AuthLibAdapter {
    override fun adapt(profile: GameProfile): SkinProfile {
        return SkinProfile(
            profile.id,
            profile.name,
            profile.properties["textures"]
        )
    }
}