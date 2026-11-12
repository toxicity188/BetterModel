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

public record KeyframeAxis(
    @NotNull Float2FloatFunction x,
    @NotNull Float2FloatFunction y,
    @NotNull Float2FloatFunction z
) implements Function<Vector3f, Vector3f> {

    public static final Float2FloatFunction OPERATOR_NONE = Float2FloatFunction.NONE;
    public static final Float2FloatFunction OPERATOR_INVERT = f -> -f;
    public static final Float2FloatFunction OPERATOR_DECREASE = f -> f - 1F;
    public static final Float2FloatFunction OPERATOR_NORMALIZE_SCALE = f -> f / MathUtil.MODEL_TO_BLOCK_MULTIPLIER;

    public static final KeyframeAxis NONE = of(OPERATOR_NONE, OPERATOR_NONE, OPERATOR_NONE);
    public static final KeyframeAxis SCALE = of(OPERATOR_DECREASE, OPERATOR_DECREASE, OPERATOR_DECREASE);

    public static @NotNull KeyframeAxis of(@NotNull Float2FloatFunction x, @NotNull Float2FloatFunction y, @NotNull Float2FloatFunction z) {
        return new KeyframeAxis(x, y, z);
    }

    @Override
    @NotNull
    public Vector3f apply(@NotNull Vector3f dest) {
        dest.x = x.applyAsFloat(dest.x);
        dest.y = y.applyAsFloat(dest.y);
        dest.z = z.applyAsFloat(dest.z);
        return dest;
    }
}
