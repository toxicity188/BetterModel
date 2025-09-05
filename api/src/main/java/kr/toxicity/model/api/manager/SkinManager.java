package kr.toxicity.model.api.manager;

import com.mojang.authlib.GameProfile;
import kr.toxicity.model.api.player.PlayerSkinProvider;
import kr.toxicity.model.api.skin.SkinData;
import org.jetbrains.annotations.NotNull;

/**
 * Skin manager
 */
public interface SkinManager {

    boolean supported();

    @NotNull SkinData getOrRequest(@NotNull GameProfile profile);

    boolean isSlim(@NotNull GameProfile profile);

    void removeCache(@NotNull GameProfile profile);

    void setSkinProvider(@NotNull PlayerSkinProvider provider);

    default void refresh(@NotNull GameProfile profile) {
        removeCache(profile);
        getOrRequest(profile);
    }
}
