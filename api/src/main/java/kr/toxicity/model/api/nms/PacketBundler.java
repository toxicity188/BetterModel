package kr.toxicity.model.api.nms;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * A packet bundler
 */
public interface PacketBundler {
    /**
     * Checks this bundler is empty
     * @return empty or not
     */
    boolean isEmpty();

    /**
     * Gets bundler's packet size.
     * @return size
     */
    int size();

    /**
     * Sends all packets to player
     * @param player target player
     */
    default void send(@NotNull Player player) {
        send(player, () -> {});
    }

    /**
     * Sends all packets to player
     * @param player target player
     * @param onSuccess listener on success
     */
    void send(@NotNull Player player, @NotNull Runnable onSuccess);
}
