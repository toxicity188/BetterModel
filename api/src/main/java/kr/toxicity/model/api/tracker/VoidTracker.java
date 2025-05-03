package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.data.renderer.RenderInstance;
import kr.toxicity.model.api.data.renderer.RenderSource;
import lombok.Setter;
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
    @Setter
    private UUID uuid = UUID.randomUUID();

    /**
     * Void tracker.
     * @param source source
     * @param instance render instance.
     * @param modifier modifier
     */
    public VoidTracker(@NotNull RenderSource.Dummy source, @NotNull RenderInstance instance, @NotNull TrackerModifier modifier) {
        super(source, instance, modifier);
        this.location = source.location();
        instance.animate("spawn");
        instance.scale(modifier.scale());
        rotation(() -> new ModelRotation(0, this.location.getYaw()));
        update();
    }

    /**
     * Moves model to another location.
     * @param location location
     */
    public void location(@NotNull Location location) {
        this.location = Objects.requireNonNull(location, "location");
        var bundler = BetterModel.inst().nms().createBundler();
        instance.teleport(location, bundler);
        if (!bundler.isEmpty()) viewedPlayer().forEach(bundler::send);
    }

    @NotNull
    @Override
    public UUID uuid() {
        return uuid;
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
     * Spawns model to some player
     * @param player player
     */
    public void spawn(@NotNull Player player) {
        var bundler = BetterModel.inst().nms().createBundler();
        spawn(player, bundler);
        bundler.send(player);
    }
}
