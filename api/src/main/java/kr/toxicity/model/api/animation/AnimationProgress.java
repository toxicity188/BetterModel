/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.animation;

import kr.toxicity.model.api.animation.keyframe.KeyframeRuntimeContext;
import kr.toxicity.model.api.bone.BoneMovement;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents the state of an animation at a specific keyframe.
 * <p>
 * This interface defines how to apply the keyframe's transformation to a bone's movement.
 * </p>
 *
 * @since 2.0.0
 */
public interface AnimationProgress extends Timed {
    /**
     * An empty animation progress that applies no transformation.
     * @since 2.0.0
     */
    AnimationProgress EMPTY = empty(0);

    /**
     * Checks if interpolation should be skipped after this keyframe.
     *
     * @return true to skip interpolation, false otherwise
     * @since 2.0.0
     */
    boolean skipInterpolation();

    /**
     * Checks if the rotation in this keyframe should be applied globally.
     *
     * @return true for global rotation, false for local
     * @since 2.0.0
     */
    boolean globalRotation();

    /**
     * Creates an empty animation progress at a specific time.
     *
     * @param time the time of the keyframe
     * @return an empty progress
     * @since 2.0.0
     */
    static @NotNull AnimationProgress empty(float time) {
        return new EmptyProgress(time, false, false);
    }

    /**
     * Creates an empty animation progress at a specific time.
     *
     * @param time the time of the keyframe
     * @param skipInterpolation whether to skip interpolation
     * @param globalRotation the rotation mode
     * @return an empty progress
     * @since 3.4.0
     */
    static @NotNull AnimationProgress empty(float time, boolean skipInterpolation, boolean globalRotation) {
        return new EmptyProgress(time, skipInterpolation, globalRotation);
    }

    /**
     * Converts this progress to empty progress at the same time.
     *
     * @return an empty progress
     * @since 2.0.0
     */
    default @NotNull AnimationProgress toEmpty() {
        var time = time();
        return time <= 0 ? EMPTY : empty(time, skipInterpolation(), globalRotation());
    }

    /**
     * Creates an empty timed storage with a start and end keyframe.
     *
     * @param time the duration of the empty animation
     * @return the timed storage
     * @since 2.0.0
     */
    static @NotNull TimedStorage<AnimationProgress> emptyStorage(float time) {
        return TimedStorage.listOf(List.of(
            EMPTY,
            empty(time)
        ));
    }

    /**
     * Evaluates this keyframe using the supplied base movement.
     *
     * @param context the runtime context for evaluating keyframe expressions
     * @param movement the base movement
     * @param dest mutable storage for the result
     * @return the destination containing the evaluated movement
     */
    @NotNull BoneMovement animate(@NotNull KeyframeRuntimeContext context, @NotNull BoneMovement movement, @NotNull BoneMovement dest);

    /**
     * Represents a keyframe that copies the base movement to the destination.
     *
     * @param time the time in seconds
     * @param skipInterpolation whether to skip interpolation
     * @param globalRotation whether the rotation is in global coordinates
     */
    record EmptyProgress(float time, boolean skipInterpolation, boolean globalRotation) implements AnimationProgress {

        @Override
        public @NotNull BoneMovement animate(@NotNull KeyframeRuntimeContext context, @NotNull BoneMovement movement, @NotNull BoneMovement dest) {
            return dest.set(movement);
        }

        @Override
        public @NotNull AnimationProgress toEmpty() {
            return this;
        }
    }
}
