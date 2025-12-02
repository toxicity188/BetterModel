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
 * Profile manager
 */
public interface ProfileManager {

    /**
     * Gets current supplier
     * @return current supplier
     */
    @NotNull ModelProfileSupplier supplier();

    /**
     * Loads skin from raw textures
     * @param rawTextures textures
     * @return skin
     */
    @NotNull ModelProfileSkin skin(@NotNull String rawTextures);

    /**
     * Sets supplier
     * @param supplier new supplier
     */
    void supplier(@NotNull ModelProfileSupplier supplier);
}
