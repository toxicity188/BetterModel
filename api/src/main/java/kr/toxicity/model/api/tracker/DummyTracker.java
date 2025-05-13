package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.data.renderer.RenderInstance;
import kr.toxicity.model.api.data.renderer.RenderSource;
import kr.toxicity.model.api.event.CreateDummyTrackerEvent;
import kr.toxicity.model.api.nms.PlayerChannelHandler;
import kr.toxicity.model.api.util.EventUtil;
import kr.toxicity.model.api.util.FunctionUtil;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Objects;
import java.util.UUID;

/**
 * No tracking tracker.
 */
public final class DummyTracker extends Tracker {
    private Location location;
    @Setter
    private UUID uuid = UUID.randomUUID();

    /**
     * Void tracker.
     * @param source source
     * @param instance render instance.
     * @param modifier modifier
     */
    public DummyTracker(@NotNull RenderSource.Located source, @NotNull RenderInstance instance, @NotNull TrackerModifier modifier) {
        super(source, instance, modifier);
        this.location = source.location();
        instance.animate("spawn");
        instance.scale(modifier.scale());
        rotation(() -> new ModelRotation(0, this.location.getYaw()));
        instance.defaultPosition(FunctionUtil.asSupplier(new Vector3f()));
        update();
        EventUtil.call(new CreateDummyTrackerEvent(this));
    }

    /**
     * Moves model to another location.
     * @param location location
     */
    public void location(@NotNull Location location) {
        this.location = Objects.requireNonNull(location, "location");
        var bundler = instance.createBundler();
        instance.teleport(location, bundler);
        if (!bundler.isEmpty()) instance.allPlayer()
                .map(PlayerChannelHandler::player)
                .forEach(bundler::send);
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
        var bundler = instance.createBundler();
        spawn(player, bundler);
        bundler.send(player);
    }
}
