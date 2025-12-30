/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.manager;

import kr.toxicity.model.api.profile.ModelProfileSkin;
import kr.toxicity.model.api.profile.ModelProfileSupplier;
import org.jetbrains.annotations.NotNull;

/**
 * Manages the resolution and creation of model profiles (skins).
 * <p>
 * This manager allows configuring the strategy for fetching profiles (e.g., from Mojang API, cache, or custom sources)
 * and creating skin instances from raw texture data.
 * </p>
 *
 * @since 1.15.2
 */
public interface ProfileManager {

    /**
     * Returns the current profile supplier strategy.
     *
     * @return the profile supplier
     * @since 1.15.2
     */
    @NotNull ModelProfileSupplier supplier();

    /**
     * Creates a {@link ModelProfileSkin} from a raw texture string (Base64 encoded).
     *
     * @param rawTextures the raw texture data
     * @return the created skin profile
     * @since 1.15.2
     */
    @NotNull ModelProfileSkin skin(@NotNull String rawTextures);

    /**
     * Sets the profile supplier strategy.
     *
     * @param supplier the new profile supplier
     * @since 1.15.2
     */
    void supplier(@NotNull ModelProfileSupplier supplier);
}
