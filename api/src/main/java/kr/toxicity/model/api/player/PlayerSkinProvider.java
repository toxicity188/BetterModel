package kr.toxicity.model.api.player;

import com.mojang.authlib.GameProfile;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Player skin provider
 */
public interface PlayerSkinProvider {
    /**
     * Default provider
     */
    PlayerSkinProvider DEFAULT = CompletableFuture::completedFuture;

    /**
     * Gets skin profile from original profile
     * @param profile original profile
     * @return provided profile
     */
    @NotNull CompletableFuture<GameProfile> provide(@NotNull GameProfile profile);
}
