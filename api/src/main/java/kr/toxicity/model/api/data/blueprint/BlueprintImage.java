/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.blueprint;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an image file to be generated as part of the resource pack.
 * <p>
 * This record holds the image's name, its binary content, and an optional .mcmeta file for animations.
 * </p>
 *
 * @param name the name of the image file (including extension)
 * @param image the binary content of the image
 * @param mcmeta the JSON object for the .mcmeta file, if any
 * @since 1.15.2
 */
public record BlueprintImage(@NotNull String name, byte[] image, @Nullable JsonObject mcmeta) {
    /**
     * Returns the estimated size of the image in bytes.
     *
     * @return the image size
     * @since 1.15.2
     */
    public long estimatedSize() {
        return image.length;
    }
}
