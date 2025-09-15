/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.blueprint;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;

/**
 * Blueprint image.
 * @param name image name
 * @param image buffered image
 * @param mcmeta mcmeta
 */
public record BlueprintImage(@NotNull String name, @NotNull BufferedImage image, @Nullable JsonObject mcmeta) {
    /**
     * Gets estimated size
     * @return estimated size
     */
    public long estimatedSize() {
        return 4L * image.getWidth() * image.getHeight();
    }
}
