/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.manager;

import kr.toxicity.model.api.profile.ModelProfile;
import kr.toxicity.model.api.skin.SkinData;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Manages the retrieval and caching of player skins.
 * <p>
 * This manager handles fetching skin data from various sources (e.g., Mojang, SkinsRestorer)
 * and provides a fallback skin when a profile cannot be resolved.
 * </p>
 *
 * @since 1.15.2
 */
public interface SkinManager {

    /**
     * Checks if a skin provider (like SkinsRestorer) is supported and available.
     *
     * @return true if supported, false otherwise
     * @since 1.15.2
     */
    boolean supported();

    /**
     * Returns the fallback skin data used when a skin cannot be resolved.
     *
     * @return the fallback skin data
     * @since 1.15.2
     */
    @NotNull SkinData fallback();

    /**
     * Asynchronously completes an uncompleted model profile to retrieve its skin data.
     *
     * @param profile the uncompleted profile
     * @return a future that completes with the skin data
     * @since 1.15.2
     */
    @NotNull CompletableFuture<? extends SkinData> complete(@NotNull ModelProfile.Uncompleted profile);

    /**
     * Removes a specific profile from the skin cache.
     *
     * @param profile the profile to remove
     * @since 1.15.2
     */
    void removeCache(@NotNull ModelProfile profile);
}
