/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.profile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Profile info
 * @param id id
 * @param name name
 */
public record ModelProfileInfo(@NotNull UUID id, @Nullable String name) {

    /**
     * Unknown info
     */
    public static final ModelProfileInfo UNKNOWN = new ModelProfileInfo(
        UUID.fromString("00000000-0000-0000-0000-000000000000"),
        null
    );
}
