/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.animation.AnimationModifier;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A record representing an animation configuration for a {@link Tracker}.
 * <p>
 * This class defines how an animation should be applied, its priority, and the tasks to run
 * during different lifecycle stages of the animation.
 *
 * @param <T> the type of tracker this animation applies to
 * @param name the unique name of the animation
 * @param priority the priority of the animation (higher values usually take precedence)
 * @param targetClass the class type of the tracker
 * @param applyCondition a condition to check if the animation can be played
 * @param modifierBuilder a function that provides an {@link AnimationModifier}
 * @param removeTask the task to run when the animation is removed
 * @param successTask the task to run when the animation starts successfully
 * @param fallbackTask the task to run when the animation fails to start
 * @since 2.2.0
 */
public record TrackerAnimation<T extends Tracker>(
    @NotNull String name,
    int priority,
    @NotNull Class<T> targetClass,
    @NotNull Predicate<? super T> applyCondition,
    @NotNull Function<? super T, AnimationModifier> modifierBuilder,
    @NotNull Consumer<? super T> removeTask,
    @NotNull Consumer<? super T> successTask,
    @NotNull Consumer<? super T> fallbackTask
) implements Comparable<TrackerAnimation<T>> {

    private static final Comparator<TrackerAnimation<?>> COMPARATOR = Comparator.comparing((TrackerAnimation<?> animation) -> animation.priority)
        .thenComparing(animation -> animation.name);

    /**
     * Creates a new builder for a {@link TrackerAnimation}.
     *
     * @param name the name of the animation
     * @return a new builder instance
     * @since 2.2.0
     */
    public static @NotNull Builder<Tracker> builder(@NotNull String name) {
        return new Builder<>(name, Tracker.class);
    }

    @ApiStatus.Internal
    public TrackerAnimation {
    }

    @Override
    public int compareTo(@NonNull TrackerAnimation<T> o) {
        return COMPARATOR.compare(this, o);
    }

    boolean play(@NotNull Tracker tracker) {
        return play(tracker, () -> {});
    }

    boolean play(@NotNull Tracker tracker, @NotNull Runnable removeTask) {
        if (!targetClass.isInstance(tracker)) return false;
        var cast = targetClass.cast(tracker);
        if (!applyCondition.test(cast)) return false;
        var result = cast.animate(
            name,
            modifierBuilder.apply(cast),
            () -> {
                this.removeTask.accept(cast);
                removeTask.run();
            }
        );
        if (result) successTask.accept(cast);
        else fallbackTask.accept(cast);
        return result;
    }

    /**
     * A builder class for creating instances of {@link TrackerAnimation}.
     *
     * @param <T> the type of tracker
     * @since 2.2.0
     */
    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    public static final class Builder<T extends Tracker> {
        private final String name;
        private final Class<T> targetClass;

        private int priority = 0;
        private @NotNull Predicate<? super T> applyCondition = _ -> true;
        private @NotNull Function<? super T, AnimationModifier> modifierBuilder = _ -> AnimationModifier.DEFAULT;
        private @NotNull Consumer<? super T> removeTask = _ -> {};
        private @NotNull Consumer<? super T> successTask = _ -> {};
        private @NotNull Consumer<? super T> fallbackTask = _ -> {};

        /**
         * Changes the target tracker type for this builder.
         *
         * @param newTargetClass the new target class
         * @param <R> the new tracker type
         * @return a new builder for the specified type
         * @since 2.2.0
         */
        public <R extends T> Builder<R> type(@NotNull Class<R> newTargetClass) {
            return new Builder<>(name, newTargetClass)
                .priority(priority)
                .check(applyCondition)
                .modifier(modifierBuilder)
                .onRemove(removeTask)
                .onSuccess(successTask)
                .onFallback(fallbackTask);
        }

        /**
         * Sets the priority of the animation.
         *
         * @param priority the priority
         * @return this builder
         * @since 2.2.0
         */
        public @NotNull Builder<T> priority(int priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Sets the condition that must be met for the animation to play.
         *
         * @param applyCondition the condition predicate
         * @return this builder
         * @since 2.2.0
         */
        public @NotNull Builder<T> check(@NotNull Predicate<? super T> applyCondition) {
            this.applyCondition = Objects.requireNonNull(applyCondition, "applyCondition cannot be null");
            return this;
        }

        /**
         * Sets the modifier builder for the animation.
         *
         * @param modifierBuilder a function that provides an {@link AnimationModifier}
         * @return this builder
         * @since 2.2.0
         */
        public @NotNull Builder<T> modifier(@NotNull Function<? super T, AnimationModifier> modifierBuilder) {
            this.modifierBuilder = Objects.requireNonNull(modifierBuilder, "modifierBuilder cannot be null");
            return this;
        }

        /**
         * Sets the task to run when the animation is removed.
         *
         * @param removeTask the removal task
         * @return this builder
         * @since 2.2.0
         */
        public @NotNull Builder<T> onRemove(@NotNull Consumer<? super T> removeTask) {
            this.removeTask = Objects.requireNonNull(removeTask, "removeTask cannot be null");
            return this;
        }

        /**
         * Sets the task to run when the animation starts successfully.
         *
         * @param successTask the success task
         * @return this builder
         * @since 2.2.0
         */
        public @NotNull Builder<T> onSuccess(@NotNull Consumer<? super T> successTask) {
            this.successTask = Objects.requireNonNull(successTask, "successTask cannot be null");
            return this;
        }

        /**
         * Sets the task to run when the animation fails to start.
         *
         * @param fallbackTask the fallback task
         * @return this builder
         * @since 2.2.0
         */
        public @NotNull Builder<T> onFallback(@NotNull Consumer<? super T> fallbackTask) {
            this.fallbackTask = Objects.requireNonNull(fallbackTask, "fallbackTask cannot be null");
            return this;
        }

        /**
         * Builds the {@link TrackerAnimation} instance.
         *
         * @return the constructed animation
         * @since 2.2.0
         */
        public @NotNull TrackerAnimation<T> build() {
            return new TrackerAnimation<>(
                name,
                priority,
                targetClass,
                applyCondition,
                modifierBuilder,
                removeTask,
                successTask,
                fallbackTask
            );
        }
    }
}
