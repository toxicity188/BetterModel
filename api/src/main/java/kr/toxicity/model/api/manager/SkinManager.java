package kr.toxicity.model.api.manager;

import com.mojang.authlib.GameProfile;
import kr.toxicity.model.api.player.PlayerSkinProvider;
import kr.toxicity.model.api.skin.SkinData;
import org.jetbrains.annotations.NotNull;

public interface SkinManager extends GlobalManager {
    boolean supported();
    @NotNull SkinData getOrRequest(@NotNull GameProfile profile);
    boolean isSlim(@NotNull GameProfile profile);
    boolean removeCache(@NotNull GameProfile profile);
    void setSkinProvider(@NotNull PlayerSkinProvider provider);
}
