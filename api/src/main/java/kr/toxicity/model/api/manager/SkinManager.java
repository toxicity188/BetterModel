package kr.toxicity.model.api.manager;

import com.mojang.authlib.GameProfile;
import kr.toxicity.model.api.skin.SkinData;
import org.jetbrains.annotations.NotNull;

public interface SkinManager extends GlobalManager {
    boolean supported();
    @NotNull SkinData getOrCreate(@NotNull GameProfile profile);
}
