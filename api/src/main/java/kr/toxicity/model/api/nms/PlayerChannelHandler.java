package kr.toxicity.model.api.nms;

import com.mojang.authlib.GameProfile;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.tracker.EntityTrackerRegistry;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * A player channel
 */
public interface PlayerChannelHandler extends AutoCloseable {
    /**
     * Gets Bukkit player
     * @return player
     */
    @NotNull Player player();

    /**
     * Gets player's uuid
     * @return uuid
     */
    @NotNull UUID uuid();

    /**
     * Gets player game profile
     * @return game profile
     */
    default @NotNull GameProfile profile() {
        return BetterModel.plugin().nms().profile(player());
    }

    /**
     * Checks this player's skin is slim
     * @return slim or wide
     */
    boolean isSlim();

    /**
     * Sends correct entity data of this tracker
     * @param registry registry
     */
    void sendEntityData(@NotNull EntityTrackerRegistry registry);

    @Override
    void close();
}
