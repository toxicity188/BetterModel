/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.platform;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a player in the underlying platform.
 * <p>
 * This interface combines the properties of a living entity and an offline player,
 * providing access to player-specific data like name and online status.
 * </p>
 *
 * @since 2.0.0
 */
public interface PlatformPlayer extends PlatformLivingEntity, PlatformOfflinePlayer {

    /**
     * Returns the name of the player.
     *
     * @return the player name
     * @since 2.0.0
     */
    @Override
    @NotNull String name();
}
