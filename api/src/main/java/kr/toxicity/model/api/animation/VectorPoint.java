/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
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
 * Vector point
 * @param function function
 * @param time time
 * @param bezier bezier config
 * @param interpolator interpolator
 */
public record VectorPoint(@NotNull FloatFunction<Vector3f> function, float time, @NotNull BezierConfig bezier, @NotNull VectorInterpolator interpolator) implements Timed {

    private static final Vector3f ZERO = new Vector3f();

    /**
     * Empty point
     */
    public static final VectorPoint EMPTY = new VectorPoint(
        FloatFunction.of(ZERO),
        0F,
        new BezierConfig(null, null, null, null),
        VectorInterpolator.LINEAR
    );

    /**
     * Gets vector by time
     * @param time time
     * @return vector
     */
    public @NotNull Vector3f vector(float time) {
        return function.apply(time);
    }

    /**
     * Checks this vector point is continuous
     * @return is continuous
     */
    public boolean isContinuous() {
        return interpolator.isContinuous();
    }

    /**
     * Gets vector
     * @return vector
     */
    public @NotNull Vector3f vector() {
        return vector(time);
    }

    /**
     * Bezier config
     * @param leftTime left time
     * @param leftValue left value
     * @param rightTime right time
     * @param rightValue right value
     */
    public record BezierConfig(@Nullable Vector3f leftTime, @Nullable Vector3f leftValue, @Nullable Vector3f rightTime, @Nullable Vector3f rightValue) {

        @Override
        public @NotNull Vector3f leftTime() {
            return leftTime != null ? leftTime : ZERO;
        }

        @Override
        public @NotNull Vector3f leftValue() {
            return leftValue != null ? leftValue : ZERO;
        }

        @Override
        public @NotNull Vector3f rightTime() {
            return rightTime != null ? rightTime : ZERO;
        }

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
