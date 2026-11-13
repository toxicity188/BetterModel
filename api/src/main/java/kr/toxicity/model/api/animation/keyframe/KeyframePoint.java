/*
 * This source file is part of BetterModel.
 * Copyright (c) 2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.animation.keyframe;

import kr.toxicity.model.api.animation.Timed;
import kr.toxicity.model.api.util.interpolator.VectorInterpolator;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public final class KeyframePoint implements Timed {

    private static final Vector3f ZERO = new Vector3f();
    public static final KeyframePoint EMPTY = new KeyframePoint(
        KeyframeDatapointVector.ZERO,
        KeyframeAxis.NONE,
        0F,
        new BezierConfig(null, null, null, null),
        VectorInterpolator.LINEAR
    );

    private final @NotNull KeyframeDatapointVector datapoint;
    private final @NotNull KeyframeAxis axis;
    private final float time;
    private final @NotNull BezierConfig bezier;
    private final @NotNull VectorInterpolator interpolator;

    private final Map<KeyframeCompileContext, KeyframeVector> memoize = new ConcurrentHashMap<>();

    public @NotNull KeyframeVector vector(float time) {
        return memoize.computeIfAbsent(
            new KeyframeCompileContext(time),
            ctx -> datapoint.compile(ctx).withAxis(axis)
        );
    }

    public @NotNull KeyframeVector vector() {
        return vector(time);
    }

    public boolean isConstant() {
        return vector().isConstant();
    }

    public boolean isContinuous() {
        return interpolator.isContinuous();
    }

    public @NotNull BezierConfig bezier() {
        return bezier;
    }

    public @NotNull VectorInterpolator interpolator() {
        return interpolator;
    }

    @Override
    public float time() {
        return time;
    }

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
}
