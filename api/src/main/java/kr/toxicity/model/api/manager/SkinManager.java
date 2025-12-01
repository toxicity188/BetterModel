/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.manager;

import kr.toxicity.model.api.profile.ModelProfile;
import kr.toxicity.model.api.skin.SkinData;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Skin manager
 */
public interface SkinManager {

    boolean supported();

    @NotNull SkinData fallback();

    @NotNull CompletableFuture<? extends SkinData> complete(@NotNull ModelProfile.Uncompleted profile);

    void removeCache(@NotNull ModelProfile profile);

    default void refresh(@NotNull ModelProfile profile) {
        removeCache(profile);
        complete(profile.asUncompleted());
    }
}
