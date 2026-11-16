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
import org.joml.Vector3f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a vector keyframe point at a specific time in an animation.
 * <pre>{@code
 * var point = new KeyframePoint(KeyframeDatapointVector.ZERO, KeyframeAxis.NONE,
 *     0.5F, KeyframePoint.EMPTY.bezier(), VectorInterpolator.LINEAR);
 * var vector = point.vector();
 * }</pre>
 *
 * @since 3.7.0
 */
@RequiredArgsConstructor
public final class KeyframePoint implements Timed {

    private static final Vector3f ZERO = new Vector3f();
    /**
     * An empty, default vector point at time 0 with linear interpolation.
     * @since 1.15.2
     */
    public static final KeyframePoint EMPTY = new KeyframePoint(
        KeyframeDatapointVector.ZERO,
        KeyframeAxis.NONE,
        0F,
        new BezierConfig(ZERO, ZERO, ZERO, ZERO),
        VectorInterpolator.LINEAR
    );

    private final @NotNull KeyframeDatapointVector datapoint;
    private final @NotNull KeyframeAxis axis;
    private final float time;
    private final @NotNull BezierConfig bezier;
    private final @NotNull VectorInterpolator interpolator;

    private final Map<KeyframeCompileContext, KeyframeVector> memoize = new ConcurrentHashMap<>();

    /**
     * Compiles and caches the vector for the supplied animation time.
     * <pre>{@code
     * var vector = point.vector(0.5F);
     * }</pre>
     *
     * @param time the animation time in seconds used to resolve time queries
     * @return the compiled vector with the point's axis mapping applied
     * @throws RuntimeException if compilation or axis mapping fails
     * @since 3.7.0
     */
    public @NotNull KeyframeVector vector(float time) {
        return memoize.computeIfAbsent(
            new KeyframeCompileContext(time),
            ctx -> datapoint.compile(ctx).withAxis(axis)
        );
    }

    /**
     * Compiles the vector at this point's time.
     * <pre>{@code
     * var vector = point.vector();
     * }</pre>
     *
     * @return the compiled vector with the point's axis mapping applied
     * @throws RuntimeException if compilation or axis mapping fails
     * @since 3.7.0
     */
    public @NotNull KeyframeVector vector() {
        return vector(time);
    }

    /**
     * Checks whether all components of the vector compiled at this point's time are constant.
     * <pre>{@code
     * boolean constant = point.isConstant();
     * }</pre>
     *
     * @return true if all three components are constant
     * @throws RuntimeException if compiling the vector or applying its axis mapping fails
     * @since 3.7.0
     */
    public boolean isConstant() {
        return vector().isConstant();
    }

    /**
     * Checks if the interpolation method for this point is continuous.
     * <pre>{@code
     * boolean continuous = point.isContinuous();
     * }</pre>
     *
     * @return true if continuous (e.g., linear), false if stepped
     * @since 1.15.2
     */
    public boolean isContinuous() {
        return interpolator.isContinuous();
    }

    /**
     * Gets the Bézier handle offsets for each axis.
     * <pre>{@code
     * float outgoingX = point.bezier().x().rightValue();
     * }</pre>
     *
     * @return the per-axis Bézier configuration
     * @since 3.7.0
     */
    public @NotNull BezierConfig bezier() {
        return bezier;
    }

    /**
     * Gets the interpolation method for this point.
     * <pre>{@code
     * var method = point.interpolator();
     * }</pre>
     *
     * @return the interpolation method
     * @since 3.7.0
     */
    public @NotNull VectorInterpolator interpolator() {
        return interpolator;
    }

    /**
     * Gets the time of this point in the animation timeline.
     * <pre>{@code
     * float seconds = point.time();
     * }</pre>
     *
     * @return the keyframe time in seconds
     * @since 3.7.0
     */
    @Override
    public float time() {
        return time;
    }

    /**
     * Configuration for Bézier interpolation with separate handle offsets for each axis.
     * <pre>{@code
     * var xHandles = point.bezier().x();
     * }</pre>
     *
     * @param x the handle offsets for the X axis
     * @param y the handle offsets for the Y axis
     * @param z the handle offsets for the Z axis
     * @since 1.15.2
     */
    public record BezierConfig(
        @NotNull Value x,
        @NotNull Value y,
        @NotNull Value z
    ) {
        /**
         * Creates per-axis Bézier handles from the components of four offset vectors.
         * <pre>{@code
         * var zero = new Vector3f();
         * var handles = new KeyframePoint.BezierConfig(zero, zero, zero, zero);
         * }</pre>
         *
         * @param leftTime the time offsets for the incoming (left) handle
         * @param leftValue the value offsets for the incoming (left) handle
         * @param rightTime the time offsets for the outgoing (right) handle
         * @param rightValue the value offsets for the outgoing (right) handle
         * @throws NullPointerException if any offset vector is null
         * @since 3.7.0
         */
        public BezierConfig(
            @NotNull Vector3f leftTime,
            @NotNull Vector3f leftValue,
            @NotNull Vector3f rightTime,
            @NotNull Vector3f rightValue
        ) {
            this(
                new Value(leftTime.x, leftValue.x, rightTime.x, rightValue.x),
                new Value(leftTime.y, leftValue.y, rightTime.y, rightValue.y),
                new Value(leftTime.z, leftValue.z, rightTime.z, rightValue.z)
            );
        }

        /**
         * Bézier handle offsets for one axis.
         * <pre>{@code
         * var handle = new KeyframePoint.BezierConfig.Value(0F, 0F, 0F, 0F);
         * }</pre>
         *
         * @param leftTime the time offset for the incoming (left) handle
         * @param leftValue the value offset for the incoming (left) handle
         * @param rightTime the time offset for the outgoing (right) handle
         * @param rightValue the value offset for the outgoing (right) handle
         * @since 3.7.0
         */
        public record Value(
            float leftTime,
            float leftValue,
            float rightTime,
            float rightValue
        ) {}
    }

}
