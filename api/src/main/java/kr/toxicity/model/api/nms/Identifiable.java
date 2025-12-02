/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.nms;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Identifiable entity
 */
public interface Identifiable {

    /**
     * Gets entity id
     * @return id
     */
    int id();

    /**
     * Gets entity uuid
     * @return uuid
     */
    @NotNull UUID uuid();
}
