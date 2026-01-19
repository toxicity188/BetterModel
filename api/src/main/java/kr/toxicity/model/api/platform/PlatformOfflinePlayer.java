/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.platform;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Represents an offline player in the underlying platform.
 * <p>
 * This interface provides access to basic player identification data, such as UUID and name,
 * without requiring the player to be online.
 * </p>
 *
 * @since 2.0.0
 */
public interface PlatformOfflinePlayer {

    /**
     * Returns the unique identifier of the player.
     *
     * @return the UUID
     * @since 2.0.0
     */
    @NotNull UUID uuid();

    /**
     * Returns the name of the player, if known.
     *
     * @return the player name, or null if unknown
     * @since 2.0.0
     */
    @Nullable String name();
}
