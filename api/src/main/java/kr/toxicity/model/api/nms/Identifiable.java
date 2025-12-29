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
 * Represents an object that can be identified by a unique ID and a UUID.
 * <p>
 * This is commonly used for entities and other tracked objects in the game.
 * </p>
 *
 * @since 1.15.2
 */
public interface Identifiable {

    /**
     * Returns the integer ID of the object (e.g., entity ID).
     *
     * @return the ID
     * @since 1.15.2
     */
    int id();

    /**
     * Returns the UUID of the object.
     *
     * @return the UUID
     * @since 1.15.2
     */
    @NotNull UUID uuid();
}
