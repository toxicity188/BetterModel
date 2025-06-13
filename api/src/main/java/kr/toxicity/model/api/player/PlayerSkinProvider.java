package kr.toxicity.model.api.player;

import com.mojang.authlib.GameProfile;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public interface PlayerSkinProvider {
    PlayerSkinProvider DEFAULT = CompletableFuture::completedFuture;
    @NotNull CompletableFuture<GameProfile> provide(@NotNull GameProfile profile);
}
