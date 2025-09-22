/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.raw;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * A texture resolution of a model
 * @param width width
 * @param height height
 */
@ApiStatus.Internal
public record ModelResolution(
        int width,
        int height
) {
    /**
     * Selects correct resolution to use
     * @param other another resolution
     * @return correct resolution
     */
    public @NotNull ModelResolution then(@NotNull ModelResolution other) {
        return new ModelResolution(
                Math.max(width, other.width),
                Math.max(height, other.height)
        );
    }
}
