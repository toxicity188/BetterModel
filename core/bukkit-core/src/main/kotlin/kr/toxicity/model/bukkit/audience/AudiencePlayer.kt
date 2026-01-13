package kr.toxicity.model.bukkit.audience

import kr.toxicity.model.api.platform.PlatformPlayer
import net.kyori.adventure.audience.Audience

@Suppress("JavaDefaultMethodsNotOverriddenByDelegation")
data class AudiencePlayer(
    val player: PlatformPlayer,
    val audience: Audience
) : Audience by audience
