/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.animation.keyframe;

import kr.toxicity.model.api.util.MathUtil;
import kr.toxicity.model.api.util.function.Float2FloatFunction;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.function.Function;

/**
 * A set of scalar transformations applied independently to the three components of a keyframe vector.
 * <pre>{@code
 * var invertedX = KeyframeAxis.of(KeyframeAxis.OPERATOR_INVERT,
 *     KeyframeAxis.OPERATOR_NONE, KeyframeAxis.OPERATOR_NONE);
 * }</pre>
 *
 * @param x the X transformation
 * @param y the Y transformation
 * @param z the Z transformation
 * @since 3.7.0
 */
public record KeyframeAxis(
    @NotNull Float2FloatFunction x,
    @NotNull Float2FloatFunction y,
    @NotNull Float2FloatFunction z
) implements Function<Vector3f, Vector3f> {

    /**
     * Leaves the component unchanged.
     * @since 3.7.0
     */
    public static final Float2FloatFunction OPERATOR_NONE = Float2FloatFunction.NONE;
    /**
     * Negates the component.
     * @since 3.7.0
     */
    public static final Float2FloatFunction OPERATOR_INVERT = f -> -f;
    /**
     * Subtracts one from the component.
     * @since 3.7.0
     */
    public static final Float2FloatFunction OPERATOR_DECREASE = f -> f - 1F;
    /**
     * Converts a component from model units to block units.
     * @since 3.7.0
     */
    public static final Float2FloatFunction OPERATOR_NORMALIZE_SCALE = f -> f / MathUtil.MODEL_TO_BLOCK_MULTIPLIER;

    /**
     * Leaves all three components unchanged.
     * @since 3.7.0
     */
    public static final KeyframeAxis NONE = of(OPERATOR_NONE, OPERATOR_NONE, OPERATOR_NONE);
    /**
     * Converts scale factors to offsets by subtracting one from each component.
     * @since 3.7.0
     */
    public static final KeyframeAxis SCALE = of(OPERATOR_DECREASE, OPERATOR_DECREASE, OPERATOR_DECREASE);

    /**
     * Creates a set of per-axis transformations.
     * <pre>{@code
     * var axis = KeyframeAxis.of(KeyframeAxis.OPERATOR_INVERT,
     *     KeyframeAxis.OPERATOR_NONE, KeyframeAxis.OPERATOR_NONE);
     * }</pre>
     *
     * @param x the X transformation
     * @param y the Y transformation
     * @param z the Z transformation
     * @return the axis mapping
     * @since 3.7.0
     */
    public static @NotNull KeyframeAxis of(@NotNull Float2FloatFunction x, @NotNull Float2FloatFunction y, @NotNull Float2FloatFunction z) {
        return new KeyframeAxis(x, y, z);
    }

    /**
     * Applies the X, Y, and Z transformations to the supplied vector in place.
     * <pre>{@code
     * var offset = KeyframeAxis.SCALE.apply(new Vector3f(2F, 1F, 1F));
     * }</pre>
     *
     * @param dest the vector to transform
     * @return the same vector after transformation
     * @throws RuntimeException if a component transformation throws. Earlier components may already have changed
     * @since 3.7.0
     */
    @Override
    @NotNull
    public Vector3f apply(@NotNull Vector3f dest) {
        dest.x = x.applyAsFloat(dest.x);
        dest.y = y.applyAsFloat(dest.y);
        dest.z = z.applyAsFloat(dest.z);
        return dest;
    }
}
