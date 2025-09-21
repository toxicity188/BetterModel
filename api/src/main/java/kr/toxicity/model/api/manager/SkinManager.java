/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.manager;

import com.mojang.authlib.GameProfile;
import kr.toxicity.model.api.player.PlayerSkinProvider;
import kr.toxicity.model.api.skin.AuthLibAdapter;
import kr.toxicity.model.api.skin.SkinData;
import kr.toxicity.model.api.skin.SkinProfile;
import org.jetbrains.annotations.NotNull;

/**
 * Skin manager
 */
public interface SkinManager {

    @NotNull AuthLibAdapter authlib();

    boolean supported();

    void setSkinProvider(@NotNull PlayerSkinProvider provider);

    @NotNull SkinData getOrRequest(@NotNull SkinProfile profile);

    boolean isSlim(@NotNull SkinProfile profile);

    void removeCache(@NotNull SkinProfile profile);

    default void refresh(@NotNull SkinProfile profile) {
        removeCache(profile);
        getOrRequest(profile);
    }

    default @NotNull SkinData getOrRequest(@NotNull GameProfile profile) {
        return getOrRequest(authlib().adapt(profile));
    }

    default boolean isSlim(@NotNull GameProfile profile) {
        return isSlim(authlib().adapt(profile));
    }

    default void removeCache(@NotNull GameProfile profile) {
        removeCache(authlib().adapt(profile));
    }

    default void refresh(@NotNull GameProfile profile) {
        refresh(authlib().adapt(profile));
    }
}
