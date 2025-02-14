package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.data.renderer.RenderInstance;
import kr.toxicity.model.api.entity.TrackerMovement;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

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
        Supplier<TrackerMovement> supplier = () -> new TrackerMovement(
                new Vector3f(0, 0, 0F),
                new Vector3f(modifier.scale()),
                new Vector3f(0, -location.getYaw(), 0)
        );
        setMovement(supplier);
        instance.setup(getMovement().get());
    }

    /**
     * Moves model to other location.
     * @param location location
     */
    public void location(Location location) {
        this.location = Objects.requireNonNull(location, "location");
        var bundler = BetterModel.inst().nms().createBundler();
        instance.teleport(location, bundler);
        if (!bundler.isEmpty()) for (Player player : viewedPlayer()) {
            bundler.send(player);
        }
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
