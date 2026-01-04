/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.animation;

import kr.toxicity.model.api.util.function.FloatFunction;
import kr.toxicity.model.api.util.interpolator.VectorInterpolator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * Represents a keyframe point in an animation timeline.
 * <p>
 * This record holds the value of a vector (position, rotation, or scale) at a specific time,
 * along with interpolation information to create smooth transitions between keyframes.
 * </p>
 *
 * @param function a function to get the vector value, which may be dynamic (e.g., based on Molang expressions)
 * @param time the time of this keyframe in seconds
 * @param bezier the bezier curve configuration for interpolation, if applicable
 * @param interpolator the interpolation method to use (e.g., linear, bezier, catmull-rom)
 * @since 1.15.2
 */
public record VectorPoint(@NotNull FloatFunction<Vector3f> function, float time, @NotNull BezierConfig bezier, @NotNull VectorInterpolator interpolator) implements Timed {

    private static final Vector3f ZERO = new Vector3f();

    /**
     * An empty, default vector point at time 0 with linear interpolation.
     * @since 1.15.2
     */
    public static final VectorPoint EMPTY = new VectorPoint(
        FloatFunction.of(ZERO),
        0F,
        new BezierConfig(null, null, null, null),
        VectorInterpolator.LINEAR
    );

    /**
     * Gets the vector value at this keyframe's specific time.
     *
     * @return the vector value
     * @since 1.15.2
     */
    public @NotNull Vector3f vector() {
        return vector(time);
    }

    /**
     * Gets the vector value at a specific time, evaluating the function if necessary.
     *
     * @param time the time to evaluate at
     * @return the calculated vector
     * @since 1.15.2
     */
    public @NotNull Vector3f vector(float time) {
        return function.apply(time);
    }

    /**
     * Checks if the interpolation method for this point is continuous.
     *
     * @return true if continuous (e.g., linear), false if stepped
     * @since 1.15.2
     */
    public boolean isContinuous() {
        return interpolator.isContinuous();
    }

    /**
     * Configuration for bezier curve interpolation.
     *
     * @param leftTime the time offset for the incoming (left) handle
     * @param leftValue the value offset for the incoming (left) handle
     * @param rightTime the time offset for the outgoing (right) handle
     * @param rightValue the value offset for the outgoing (right) handle
     * @since 1.15.2
     */
    public record BezierConfig(@Nullable Vector3f leftTime, @Nullable Vector3f leftValue, @Nullable Vector3f rightTime, @Nullable Vector3f rightValue) {

        /**
         * Gets the time offset for the incoming (left) handle.
         * If null, returns a zero vector.
         * @return the left time offset vector
         * @since 1.15.2
         */
        @Override
        public @NotNull Vector3f leftTime() {
            return leftTime != null ? leftTime : ZERO;
        }

        /**
         * Gets the value offset for the incoming (left) handle.
         * If null, returns a zero vector.
         * @return the left value offset vector
         * @since 1.15.2
         */
        @Override
        public @NotNull Vector3f leftValue() {
            return leftValue != null ? leftValue : ZERO;
        }

        /**
         * Gets the time offset for the outgoing (right) handle.
         * If null, returns a zero vector.
         * @return the right time offset vector
         * @since 1.15.2
         */
        @Override
        public @NotNull Vector3f rightTime() {
            return rightTime != null ? rightTime : ZERO;
        }

        /**
         * Gets the value offset for the outgoing (right) handle.
         * If null, returns a zero vector.
         * @return the right value offset vector
         * @since 1.15.2
         */
        @Override
        public @NotNull Vector3f rightValue() {
            return rightValue != null ? rightValue : ZERO;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VectorPoint that)) return false;
        return Float.compare(time, that.time) == 0;
    }

    @Override
    public int hashCode() {
        return Float.hashCode(time);
    }
}
