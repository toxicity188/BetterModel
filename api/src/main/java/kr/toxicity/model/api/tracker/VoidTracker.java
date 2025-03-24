package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.data.renderer.RenderInstance;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * No tracking tracker.
 */
public final class VoidTracker extends Tracker {
    private Location location;
    private final UUID uuid;

    /**
     * Void tracker.
     * @param uuid uuid
     * @param instance render instance.
     * @param modifier modifier
     * @param location initial location.
     */
    public VoidTracker(@NotNull UUID uuid, @NotNull RenderInstance instance, @NotNull TrackerModifier modifier, @NotNull Location location) {
        super(instance, modifier);
        this.uuid = uuid;
        this.location = location;
        update();
    }

    /**
     * Moves model to other location.
     * @param location location
     */
    public void location(Location location) {
        this.location = Objects.requireNonNull(location, "location");
        var bundler = BetterModel.inst().nms().createBundler();
        instance.teleport(location, bundler);
        if (!bundler.isEmpty()) viewedPlayer().forEach(bundler::send);
    }

    @NotNull
    @Override
    public ModelRotation rotation() {
        return new ModelRotation(0, location.getYaw());
    }

    /**
     * Gets location.
     * @return location
     */
    @Override
    public @NotNull Location location() {
        return location;
    }

    /**
     * Gets uuid.
     * @return uuid
     */
    @Override
    public @NotNull UUID uuid() {
        return uuid;
    }

    /**
     * Spawns model to some player
     * @param player player
     */
    public void spawn(@NotNull Player player) {
        var bundler = BetterModel.inst().nms().createBundler();
        spawn(player, bundler);
        bundler.send(player);
    }
}
