/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.animation.AnimationModifier;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

import static kr.toxicity.model.api.util.CollectionUtil.*;

/**
 * A utility class for managing built-in animations for trackers.
 *
 * @since 2.2.0
 */
public final class TrackerBuiltInAnimation {

    private static final Map<String, TrackerAnimation<?>> BY_NAME = newAddressingMap();
    private static final SortedSet<TrackerAnimation<?>> BY_PRIORITY = new TreeSet<>();

    /**
     * Registers a new tracker animation.
     *
     * @param name             the name of the animation
     * @param builderFunction the function to build the animation
     * @param <T>              the tracker type
     * @return the registered animation
     */
    public static <T extends Tracker> @NotNull TrackerAnimation<T> register(@NotNull String name, @NotNull Function<TrackerAnimation.Builder<Tracker>, TrackerAnimation.Builder<T>> builderFunction) {
        var animation = builderFunction.apply(TrackerAnimation.builder(name)).build();
        register(animation);
        return animation;
    }

    private static void register(@NotNull TrackerAnimation<?> animation) {
        synchronized (BY_NAME) {
            if (BY_NAME.put(animation.name(), animation) != null) throw new IllegalStateException("Duplicate animation name: " + animation.name());
            BY_PRIORITY.add(animation);
        }
    }

    static void play(@NotNull Tracker tracker) {
        BY_PRIORITY.forEach(animation -> animation.play(tracker));
    }

    /**
     * The default idle animation.
     *
     * @since 2.2.0
     */
    public static final TrackerAnimation<Tracker> IDLE = register("idle", b -> b.modifier(_ -> AnimationModifier.builder()
        .start(6)
        .type(AnimationIterator.Type.LOOP)
        .build()
    ));

    /**
     * The default walk animation for entity trackers.
     *
     * @since 2.2.0
     */
    public static final TrackerAnimation<EntityTracker> WALK = register("walk", b -> b.type(EntityTracker.class)
        .modifier(tracker -> {
            var property = tracker.registry().animationProperty;
            return AnimationModifier.builder()
                .start(6)
                .predicate(property.onWalk)
                .speed(tracker.modifier.damageAnimation() ? property.walkSpeed : null)
                .type(AnimationIterator.Type.LOOP)
                .build();
        }));

    /**
     * The default flying idle animation for entity trackers.
     *
     * @since 2.2.0
     */
    public static final TrackerAnimation<EntityTracker> IDLE_FLY = register("idle_fly", b -> b.type(EntityTracker.class)
        .modifier(tracker -> {
            var property = tracker.registry().animationProperty;
            return AnimationModifier.builder()
                .start(6)
                .predicate(property.onFly)
                .type(AnimationIterator.Type.LOOP)
                .build();
        }));

    /**
     * The default flying walk animation for entity trackers.
     *
     * @since 2.2.0
     */
    public static final TrackerAnimation<EntityTracker> WALK_FLY = register("walk_fly", b -> b.type(EntityTracker.class)
        .modifier(tracker -> {
            var property = tracker.registry().animationProperty;
            return AnimationModifier.builder()
                .start(6)
                .predicate(() -> property.onFly.getAsBoolean() && property.onWalk.getAsBoolean())
                .type(AnimationIterator.Type.LOOP)
                .build();
        }));

    /**
     * The default spawn animation for entity trackers.
     *
     * @since 2.2.0
     */
    public static final TrackerAnimation<EntityTracker> SPAWN = register("spawn", b -> b.type(EntityTracker.class)
        .modifier(_ -> AnimationModifier.DEFAULT_WITH_PLAY_ONCE));

    private TrackerBuiltInAnimation() {
        throw new IllegalStateException("Utility class");
    }
}
