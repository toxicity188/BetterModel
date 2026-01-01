/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.nms;

import kr.toxicity.model.api.entity.BaseBukkitPlayer;
import kr.toxicity.model.api.tracker.EntityTrackerRegistry;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Manages the network channel for a player, allowing for packet interception and injection.
 * <p>
 * This is crucial for handling custom packets and entity tracking.
 * </p>
 *
 * @since 1.15.2
 */
public interface PlayerChannelHandler extends Identifiable, AutoCloseable {

    /**
     * Returns the Bukkit player associated with this handler.
     *
     * @return the player
     * @since 1.15.2
     */
    default @NotNull Player player() {
        return base().entity();
    }

    @Override
    default @NotNull UUID uuid() {
        return base().uuid();
    }

    @Override
    default int id() {
        return base().id();
    }

    /**
     * Returns the base player adapter.
     *
     * @return the base player
     * @since 1.15.2
     */
    @NotNull BaseBukkitPlayer base();

    /**
     * Sends the correct entity data for a specific tracker to the player.
     *
     * @param registry the entity tracker registry
     * @since 1.15.2
     */
    void sendEntityData(@NotNull EntityTrackerRegistry registry);

    /**
     * Closes the channel handler, cleaning up resources.
     *
     * @since 1.15.2
     */
    @Override
    void close();
}
