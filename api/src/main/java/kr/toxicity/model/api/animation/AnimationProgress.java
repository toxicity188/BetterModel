/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.animation;

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
        return new EmptyProgress(time);
    }

    /**
     * Converts this progress to empty progress at the same time.
     *
     * @return an empty progress
     * @since 2.0.0
     */
    default @NotNull AnimationProgress toEmpty() {
        var time = time();
        return time <= 0 ? EMPTY : empty(time);
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
     * Applies this keyframe's animation to a bone's movement.
     *
     * @param movement the current bone movement
     * @param dest the destination object to store the result
     * @return the resulting bone movement
     * @since 2.0.0
     */
    @NotNull BoneMovement animate(@NotNull BoneMovement movement, @NotNull BoneMovement dest);

    /**
     * An implementation of {@link AnimationProgress} that represents an empty keyframe.
     *
     * @param time the time of the keyframe
     * @since 2.0.0
     */
    record EmptyProgress(float time) implements AnimationProgress {

        @Override
        public @NotNull BoneMovement animate(@NotNull BoneMovement movement, @NotNull BoneMovement dest) {
            return dest.set(movement);
        }

        @Override
        public @NotNull AnimationProgress toEmpty() {
            return this;
        }

        @Override
        public boolean skipInterpolation() {
            return false;
        }

        @Override
        public boolean globalRotation() {
            return false;
        }
    }
}
