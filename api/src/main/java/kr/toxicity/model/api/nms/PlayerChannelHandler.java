package kr.toxicity.model.api.nms;

import kr.toxicity.model.api.tracker.EntityTracker;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;

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
     * Checks this player's skin is slim
     * @return slim or wide
     */
    boolean isSlim();

    /**
     * Tracks this entity tracker
     * @param tracker tracker
     */
    void startTrack(@NotNull EntityTracker tracker);

    /**
     * Stops tracking this entity tracker
     * @param tracker tracker
     */
    void endTrack(@NotNull EntityTracker tracker);

    /**
     * Gets tracked trackers.
     * @return tracker collection
     */
    @NotNull @Unmodifiable
    Collection<EntityTracker> trackedTrackers();

    /**
     * Stops tracking all entity tracker
     */
    void unregisterAll();

    /**
     * Sets whether to show some player's animation
     * @param show show or not
     */
    void showPlayerLimb(boolean show);

    /**
     * Gets whether to show some player's animation
     * @return show or not
     */
    boolean showPlayerLimb();
}
