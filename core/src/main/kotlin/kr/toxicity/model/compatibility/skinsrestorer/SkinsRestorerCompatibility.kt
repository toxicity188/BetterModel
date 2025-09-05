package kr.toxicity.model.compatibility.skinsrestorer

import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import kr.toxicity.model.compatibility.Compatibility
import kr.toxicity.model.manager.SkinManagerImpl
import kr.toxicity.model.util.PLUGIN
import net.skinsrestorer.api.SkinsRestorerProvider
import net.skinsrestorer.api.event.SkinApplyEvent
import net.skinsrestorer.api.property.SkinProperty
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

class SkinsRestorerCompatibility : Compatibility {
    override fun start() {
        SkinsRestorerProvider.get().eventBus.subscribe(
            PLUGIN,
            SkinApplyEvent::class.java
        ) {
            val player = it.getPlayer(Player::class.java)
            SkinManagerImpl.refresh(GameProfile(
                player.uniqueId,
                player.name
            ).apply {
                properties.put("textures", it.property.toProperty())
            })
        }
        SkinManagerImpl.setSkinProvider {
            CompletableFuture.completedFuture(SkinsRestorerProvider.get()
                .playerStorage
                .getSkinForPlayer(
                    it.id,
                    it.name,
                    Bukkit.getOnlineMode()
                ).map { skin ->
                    GameProfile(
                        it.id,
                        it.name
                    ).apply {
                        properties.put("textures", skin.toProperty())
                    }
                }.orElse(null))
        }
    }

    private fun SkinProperty.toProperty() = Property(
        "textures",
        value,
        signature
    )
}