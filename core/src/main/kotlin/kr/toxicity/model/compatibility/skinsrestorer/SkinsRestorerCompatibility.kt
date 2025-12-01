/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.compatibility.skinsrestorer

import kr.toxicity.model.api.profile.ModelProfile
import kr.toxicity.model.api.profile.ModelProfileInfo
import kr.toxicity.model.compatibility.Compatibility
import kr.toxicity.model.manager.ProfileManagerImpl
import kr.toxicity.model.manager.SkinManagerImpl
import kr.toxicity.model.util.PLUGIN
import net.skinsrestorer.api.SkinsRestorerProvider
import net.skinsrestorer.api.event.SkinApplyEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

class SkinsRestorerCompatibility : Compatibility {

    private val manager = SkinsRestorerProvider.get()

    override fun start() {
        SkinsRestorerProvider.get().run {
            eventBus.subscribe(
                PLUGIN,
                SkinApplyEvent::class.java
            ) {
                val player = it.getPlayer(Player::class.java)
                SkinManagerImpl.refresh(ModelProfile.of(player))
            }
            ProfileManagerImpl.supplier {
                SkinsRestorerProfile(it)
            }
        }
    }

    private inner class SkinsRestorerProfile(
        private val info: ModelProfileInfo
    ) : ModelProfile.Uncompleted {
        override fun info(): ModelProfileInfo = info

        override fun complete(): CompletableFuture<ModelProfile> = CompletableFuture.supplyAsync {
            manager.playerStorage
                .getSkinForPlayer(
                    info.id,
                    info.name,
                    Bukkit.getOnlineMode()
                ).map { skin ->
                    ModelProfile.of(
                        info,
                        ProfileManagerImpl.skin(skin.value)
                    )
                }.orElse(null)
        }

    }
}
