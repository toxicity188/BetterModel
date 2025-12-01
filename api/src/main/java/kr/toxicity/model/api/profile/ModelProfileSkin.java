/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.profile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

/**
 * Model skin
 * @param skin skin
 * @param cape cape
 * @param slim is slim model
 * @param raw raw textures
 */
public record ModelProfileSkin(
    @Nullable URI skin,
    @Nullable URI cape,
    boolean slim,
    @NotNull String raw
) {

    /**
     * Empty skin
     */
    public static final ModelProfileSkin EMPTY = new ModelProfileSkin(null, null, false, "");
}
