package kr.toxicity.model.api.manager;

import kr.toxicity.model.api.nms.PlayerChannelHandler;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Player manager
 */
public interface PlayerManager {
    /**
     * Gets player channel handler
     * @param uuid player's uuid
     * @return channel handler or null
     */
    @Nullable PlayerChannelHandler player(@NotNull UUID uuid);

    /**
     * Get or creates channel handler
     * Do not use this with fake player, instead use PlayerManager#player(UUID)
     * @param player player
     * @return channel handler
     */
    @NotNull PlayerChannelHandler player(@NotNull Player player);
}