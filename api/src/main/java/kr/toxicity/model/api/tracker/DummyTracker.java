package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.data.renderer.RenderPipeline;
import kr.toxicity.model.api.event.CreateDummyTrackerEvent;
import kr.toxicity.model.api.util.EventUtil;
import kr.toxicity.model.api.util.FunctionUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * No tracking tracker.
 */
public final class DummyTracker extends Tracker {
    private Location location;

    /**
     * Dummy tracker.
     * @param location location
     * @param pipeline render instance.
     * @param modifier modifier
     * @param preUpdateConsumer task on pre-update
     */
    public DummyTracker(@NotNull Location location, @NotNull RenderPipeline pipeline, @NotNull TrackerModifier modifier, @NotNull Consumer<DummyTracker> preUpdateConsumer) {
        super(pipeline, modifier);
        this.location = location;
        animate("spawn", AnimationModifier.DEFAULT_WITH_PLAY_ONCE);
        pipeline.scale(() -> scaler().scale(this));
        rotation(() -> new ModelRotation(this.location.getPitch(), this.location.getYaw()));
        pipeline.defaultPosition(FunctionUtil.asSupplier(new Vector3f()));
        preUpdateConsumer.accept(this);
        update();
        EventUtil.call(new CreateDummyTrackerEvent(this));
    }

    /**
     * Moves model to another location.
     * @param location location
     */
    public void location(@NotNull Location location) {
        if (this.location.equals(location)) return;
        this.location = Objects.requireNonNull(location, "location");
        var bundler = pipeline.createBundler();
        pipeline.teleport(location, bundler);
        if (!bundler.isEmpty()) pipeline.allPlayer()
                .forEach(bundler::send);
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
        var bundler = pipeline.createBundler();
        spawn(player, bundler);
        bundler.send(player);
    }
}
