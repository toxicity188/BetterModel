/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.manager;

import kr.toxicity.model.api.nms.PlayerChannelHandler;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Manages player-specific data and network channels.
 * <p>
 * This manager is responsible for injecting and retrieving {@link PlayerChannelHandler} instances,
 * which are essential for sending custom packets to players.
 * </p>
 *
 * @since 1.15.2
 */
public interface PlayerManager {
    /**
     * Retrieves the channel handler for a player by their UUID.
     *
     * @param uuid the player's UUID
     * @return the channel handler, or null if not found
     * @since 1.15.2
     */
    @Nullable PlayerChannelHandler player(@NotNull UUID uuid);

    /**
     * Gets or creates the channel handler for a player.
     * <p>
     * Note: This should not be used with fake players. Use {@link #player(UUID)} instead for those cases.
     * </p>
     *
     * @param player the player
     * @return the channel handler
     * @since 1.15.2
     */
    @NotNull PlayerChannelHandler player(@NotNull Player player);
}
