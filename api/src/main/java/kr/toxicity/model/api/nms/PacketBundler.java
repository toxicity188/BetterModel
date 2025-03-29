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
     * Copy this bundler
     * @return copy
     */
    @NotNull PacketBundler copy();

    /**
     * Sends all packet to player
     * @param player target player
     */
    void send(@NotNull Player player);
}
