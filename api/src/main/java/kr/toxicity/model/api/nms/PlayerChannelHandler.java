package kr.toxicity.model.api.nms;

import com.mojang.authlib.GameProfile;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.tracker.EntityTrackerRegistry;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.stream.Stream;

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
     * Tracks this entity tracker
     * @param registry registry
     */
    void startTrack(@NotNull EntityTrackerRegistry registry);

    /**
     * Stops tracking this entity tracker
     * @param registry registry
     */
    void endTrack(@NotNull EntityTrackerRegistry registry);

    /**
     * Gets tracked registry.
     * @return registry collection
     */
    @NotNull @Unmodifiable
    Collection<EntityTrackerRegistry> trackedRegistries();

    /**
     * Gets tracked trackers.
     * @return tracker stream
     */
    default @NotNull Stream<EntityTracker> trackedTrackers() {
        return trackedRegistries()
                .stream()
                .flatMap(r -> r.trackers().stream());
    }

    /**
     * Stops tracking all entity trackers
     */
    void unregisterAll();

    @Override
    void close();
}
