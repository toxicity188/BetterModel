/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.nms;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Collects multiple packets to be sent together to a player.
 * <p>
 * This helps optimize network traffic by grouping related updates (e.g., bone movements)
 * into a single bundle or batch.
 * </p>
 *
 * @since 1.15.2
 */
public interface PacketBundler {

    /**
     * Checks if the bundler contains no packets.
     *
     * @return true if empty, false otherwise
     * @since 1.15.2
     */
    boolean isEmpty();

    /**
     * Checks if the bundler contains at least one packet.
     *
     * @return true if not empty, false otherwise
     * @since 1.15.2
     */
    default boolean isNotEmpty() {
        return !isEmpty();
    }

    /**
     * Returns the number of packets in the bundler.
     *
     * @return the packet count
     * @since 1.15.2
     */
    int size();

    /**
     * Sends all collected packets to the specified player.
     *
     * @param player the target player
     * @since 1.15.2
     */
    default void send(@NotNull Player player) {
        send(player, () -> {});
    }

    /**
     * Sends all collected packets to the specified player and executes a callback on success.
     *
     * @param player the target player
     * @param onSuccess the callback to run after sending
     * @since 1.15.2
     */
    void send(@NotNull Player player, @NotNull Runnable onSuccess);
}
