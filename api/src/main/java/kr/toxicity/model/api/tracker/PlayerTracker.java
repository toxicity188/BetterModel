/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.data.renderer.RenderPipeline;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * A specialized {@link EntityTracker} for tracking players.
 * <p>
 * This tracker automatically configures the body rotator to player mode, ensuring correct
 * head and body rotation synchronization for player entities.
 * </p>
 *
 * @since 1.15.2
 */
public final class PlayerTracker extends EntityTracker {

    /**
     * Creates a new player tracker.
     *
     * @param registry the entity tracker registry
     * @param pipeline the render pipeline
     * @param modifier the tracker modifier
     * @param preUpdateConsumer a consumer to run before the first update
     * @since 1.15.2
     */
    @ApiStatus.Internal
    public PlayerTracker(@NotNull EntityTrackerRegistry registry, @NotNull RenderPipeline pipeline, @NotNull TrackerModifier modifier, @NotNull Consumer<EntityTracker> preUpdateConsumer) {
        super(registry, pipeline, modifier, preUpdateConsumer);
        bodyRotator().setValue(setter -> setter.setPlayerMode(true));
    }
}
