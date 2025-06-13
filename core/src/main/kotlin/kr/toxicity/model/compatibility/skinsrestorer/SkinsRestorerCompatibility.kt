package kr.toxicity.model.compatibility.skinsrestorer

import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import kr.toxicity.model.compatibility.Compatibility
import kr.toxicity.model.manager.SkinManagerImpl
import net.skinsrestorer.api.SkinsRestorerProvider
import org.bukkit.Bukkit
import java.util.concurrent.CompletableFuture

class SkinsRestorerCompatibility : Compatibility {
    override fun start() {
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
                        properties.put("textures", Property(
                            "textures",
                            skin.value,
                            skin.signature
                        ))
                    }
                }.orElse(null))
        }
    }
}