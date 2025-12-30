/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.data.renderer.RenderPipeline;
import kr.toxicity.model.api.event.CreateDummyTrackerEvent;
import kr.toxicity.model.api.util.EventUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * A tracker implementation that is not attached to any entity.
 * <p>
 * Dummy trackers are positioned at a fixed location in the world and can be moved manually.
 * They are useful for static models or models controlled entirely by scripts/plugins.
 * </p>
 *
 * @since 1.15.2
 */
public final class DummyTracker extends Tracker {
    private volatile Location location;

    /**
     * Creates a new dummy tracker.
     *
     * @param location the initial location
     * @param pipeline the render pipeline
     * @param modifier the tracker modifier
     * @param preUpdateConsumer a consumer to run before the first update
     * @since 1.15.2
     */
    public DummyTracker(@NotNull Location location, @NotNull RenderPipeline pipeline, @NotNull TrackerModifier modifier, @NotNull Consumer<DummyTracker> preUpdateConsumer) {
        super(pipeline, modifier);
        this.location = location;
        animate("spawn", AnimationModifier.DEFAULT_WITH_PLAY_ONCE);
        pipeline.scale(() -> scaler().scale(this));
        rotation(() -> new ModelRotation(this.location.getPitch(), this.location.getYaw()));
        preUpdateConsumer.accept(this);
        EventUtil.call(new CreateDummyTrackerEvent(this));
    }

    /**
     * Moves the model to a new location.
     *
     * @param location the new location
     * @since 1.15.2
     */
    public void location(@NotNull Location location) {
        Objects.requireNonNull(location, "location");
        if (this.location.equals(location)) return;
        synchronized (this) {
            this.location = location;
            var bundler = pipeline.createBundler();
            pipeline.iterateTree(b -> b.teleport(location, bundler));
            if (bundler.isNotEmpty()) pipeline.allPlayer().forEach(bundler::send);
        }
    }

    /**
     * Returns the current location of the tracker.
     *
     * @return the location
     * @since 1.15.2
     */
    @Override
    public @NotNull Location location() {
        return location;
    }

    /**
     * Spawns the model for a specific player.
     *
     * @param player the target player
     * @since 1.15.2
     */
    public void spawn(@NotNull Player player) {
        var bundler = pipeline.createBundler();
        spawn(player, bundler);
        bundler.send(player);
    }
}
