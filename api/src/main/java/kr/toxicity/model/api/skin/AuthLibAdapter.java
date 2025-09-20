package kr.toxicity.model.api.skin;

import com.mojang.authlib.GameProfile;
import org.jetbrains.annotations.NotNull;

public interface AuthLibAdapter {
    @NotNull SkinProfile adapt(@NotNull GameProfile profile);
}
