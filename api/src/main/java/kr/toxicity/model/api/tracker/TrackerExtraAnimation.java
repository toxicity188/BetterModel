/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.animation.AnimationModifier;

/**
 * A utility class that contains predefined {@link TrackerAnimation} instances for common entity actions.
 *
 * @since 2.2.0
 */
public final class TrackerExtraAnimation {

    /**
     * Animation played when an entity dies.
     * Closes the tracker and marks it for removal upon completion.
     *
     * @since 2.2.0
     */
    public static final TrackerAnimation<EntityTracker> DEATH = TrackerAnimation.builder("death")
        .type(EntityTracker.class)
        .modifier(tracker -> AnimationModifier.DEFAULT_WITH_PLAY_ONCE)
        .onRemove(Tracker::close)
        .onSuccess(tracker -> tracker.forRemoval(true))
        .build();

    /**
     * Animation played when an entity takes damage.
     *
     * @since 2.2.0
     */
    public static final TrackerAnimation<EntityTracker> DAMAGE = TrackerAnimation.builder("damage")
        .type(EntityTracker.class)
        .modifier(tracker -> AnimationModifier.DEFAULT_WITH_PLAY_ONCE)
        .build();

    /**
     * Animation played when an entity jumps.
     *
     * @since 2.2.0
     */
    public static final TrackerAnimation<EntityTracker> JUMP = TrackerAnimation.builder("jump")
        .type(EntityTracker.class)
        .modifier(tracker -> AnimationModifier.DEFAULT_WITH_PLAY_ONCE)
        .build();

    private TrackerExtraAnimation() {
        throw new IllegalStateException("Utility class");
    }
}
