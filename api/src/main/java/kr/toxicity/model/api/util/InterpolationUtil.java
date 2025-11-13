/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.util;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatSortedSet;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationMovement;
import kr.toxicity.model.api.animation.VectorPoint;
import kr.toxicity.model.api.tracker.Tracker;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.joml.Vector3f;

import java.util.List;

import static kr.toxicity.model.api.util.FunctionUtil.takeIf;
import static kr.toxicity.model.api.util.MathUtil.*;

/**
 * Interpolation util
 */
@ApiStatus.Internal
public final class InterpolationUtil {

    /**
     * No initializer
     */
    private InterpolationUtil() {
        throw new RuntimeException();
    }

    private static final float FRAME_HASH = (float) Tracker.TRACKER_TICK_INTERVAL / 1000F;
    private static final float FRAME_HASH_REVERT = 1 / FRAME_HASH;

    /**
     * Builds animation movement
     * @param position position point
     * @param rotation rotation point
     * @param scale scale point
     * @param rotationGlobal rotation global
     * @param points keyframe time set
     * @return animation movement list
     */
    @NotNull
    @Unmodifiable
    public static List<AnimationMovement> buildAnimation(
            @NotNull List<VectorPoint> position,
            @NotNull List<VectorPoint> rotation,
            @NotNull List<VectorPoint> scale,
            boolean rotationGlobal,
            @NotNull FloatSortedSet points
    ) {
        var pp = interpolatorFor(position);
        var sp = interpolatorFor(scale);
        var rp = interpolatorFor(rotation);
        var array = new AnimationMovement[points.size()];
        var before = 0F;
        var iterator = points.iterator();
        var i = 0;
        while (iterator.hasNext()) {
            var f = iterator.nextFloat();
            var pr = pp.build(f);
            var sr = sp.build(f);
            var rr = rp.build(f);
            array[i++] = new AnimationMovement(
                    roundTime(f - before),
                    takeIf(pr.vector, MathUtil::isNotZero),
                    takeIf(sr.vector, MathUtil::isNotZero),
                    takeIf(rr.vector, MathUtil::isNotZero),
                    rotationGlobal,
                    pr.skipInterpolation || sr.skipInterpolation || rr.skipInterpolation
            );
            before = f;
        }
        return List.of(array);
    }

    /**
     * Creates interpolator
     * @param vectors target vector list
     * @return point builder
     */
    public static @NotNull VectorPointBuilder interpolatorFor(@NotNull List<VectorPoint> vectors) {
        var last = vectors.isEmpty() ? VectorPoint.EMPTY : vectors.getLast();
        return vectors.size() < 2 ? f -> new VectorResult(last.vector(f)) : new VectorPointBuilder() {
            private VectorPoint p1 = VectorPoint.EMPTY;
            private VectorPoint p2 = vectors.getFirst();
            private int i = 0;
            private float t = p2.time();

            @Override
            public @NotNull VectorResult build(float nextFloat) {
                while (i < vectors.size() - 1 && t < nextFloat) {
                    p1 = p2;
                    t = (p2 = vectors.get(++i)).time();
                }
                if (nextFloat > last.time()) return new VectorResult(last.vector(nextFloat));
                else return nextFloat == t ? new VectorResult(p2.vector(), !p1.isContinuous()) : new VectorResult(p1.interpolator().interpolate(vectors, i, nextFloat));
            }
        };
    }

    /**
     * Rounds float to frame time
     * @param time time
     * @return rounded time
     */
    public static float roundTime(float time) {
        return (int) fma(time, FRAME_HASH_REVERT, FRAME_EPSILON) * FRAME_HASH;
    }

    /**
     * Inserts lerp frame to given set
     * @param frames target set
     */
    public static void insertLerpFrame(@NotNull FloatSortedSet frames) {
        insertLerpFrame(frames, (float) BetterModel.config().lerpFrameTime() / 20F);
    }

    /**
     * Inserts lerp frame to given set
     * @param frames target set
     * @param frame frame
     */
    public static void insertLerpFrame(@NotNull FloatSortedSet frames, float frame) {
        if (frame <= 0F) return;
        var first = 0F;
        var second = 0F;
        var iterator = new FloatArrayList(frames).iterator();
        while (iterator.hasNext()) {
            first = second;
            second = iterator.nextFloat();
            var max = (int) ((second - first) / frame);
            for (int i = 0; i < max; i++) {
                var add = fma(frame, i + 1, first);
                if (second - add < frame + FRAME_EPSILON) continue;
                frames.add(add);
            }
        }
    }

    /**
     * Finds alpha value
     * @param p0 p0
     * @param p1 p1
     * @param alpha target value between p0 and p1
     * @return alpha (0..1)
     */
    public static float alpha(float p0, float p1, float alpha) {
        var div = p1 - p0;
        return div == 0 ? 0 : (alpha - p0) / div;
    }

    /**
     * Lerps two point
     * @param p0 p0
     * @param p1 p1
     * @param alpha alpha
     * @return lerped vector
     */
    public static @NotNull Vector3f lerp(@NotNull Vector3f p0, @NotNull Vector3f p1, float alpha) {
        return lerp(p0, p1, alpha, new Vector3f());
    }

    /**
     * Lerps two point
     * @param p0 p0
     * @param p1 p1
     * @param alpha alpha
     * @param dest destination vector
     * @return lerped vector
     */
    public static @NotNull Vector3f lerp(@NotNull Vector3f p0, @NotNull Vector3f p1, float alpha, @NotNull Vector3f dest) {
        dest.x = lerp(p0.x, p1.x, alpha);
        dest.y = lerp(p0.y, p1.y, alpha);
        dest.z = lerp(p0.z, p1.z, alpha);
        return dest;
    }

    /**
     * Lerps two point
     * @param p0 p0
     * @param p1 p1
     * @param alpha alpha
     * @return lerped point
     */
    public static float lerp(float p0, float p1, float alpha) {
        return fma(p1 - p0, alpha, p0);
    }

    private static float cubicBezier(float p0, float p1, float p2, float p3, float t) {
        float u = 1.0F - t;
        float uu = u * u;
        float tt = t * t;
        float uuu = uu * u;
        float utt = u * tt;
        float uut = uu * t;
        float ttt = tt * t;
        return fma(uuu, p0, fma(3.0F * uut, p1, fma(3.0F * utt, p2, ttt * p3)));
    }

    private static float derivativeBezier(float p0, float p1, float p2, float p3, float t) {
        float u = 1.0F - t;
        float uu = u * u;
        float ut = u * t;
        float tt = t * t;
        return fma(3.0F * uu, p1 - p0, fma(6.0F * ut, p2 - p1, 3.0F * tt * (p3 - p2)));
    }

    private static float solveBezierTForTime(float time, float t0, float h1, float h2, float t1) {
        float t = 0.5F;
        int maxIterations = 20;
        for (int i = 0; i < maxIterations; i++) {
            float bezTime = cubicBezier(t0, h1, h2, t1, t);
            float derivative = derivativeBezier(t0, h1, h2, t1, t);
            float error = bezTime - time;
            if (Math.abs(error) < FLOAT_COMPARISON_EPSILON) {
                return t;
            }
            if (derivative != 0) {
                t -= error / derivative;
            }
            t = Math.clamp(t, 0F, 1F);
        }

        return t;
    }

    /**
     * Interpolates two vectors as bezier
     * @param time time
     * @param startTime start time
     * @param endTime end time
     * @param startValue start value
     * @param endValue end value
     * @param bezierLeftTime bezier left time
     * @param bezierLeftValue bezier left value
     * @param bezierRightTime bezier right time
     * @param bezierRightValue bezier right value
     * @return interpolated vector
     */
    public static @NotNull Vector3f bezier(
            float time,
            float startTime,
            float endTime,
            @NotNull Vector3f startValue,
            @NotNull Vector3f endValue,
            @Nullable Vector3f bezierLeftTime,
            @Nullable Vector3f bezierLeftValue,
            @Nullable Vector3f bezierRightTime,
            @Nullable Vector3f bezierRightValue
    ) {
        Vector3f p1 = bezierRightValue != null ? bezierRightValue.add(startValue, new Vector3f()) : startValue;
        Vector3f p2 = bezierLeftValue != null ? bezierLeftValue.add(endValue, new Vector3f()) : endValue;
        return new Vector3f(
                cubicBezier(startValue.x, p1.x, p2.x, endValue.x, solveBezierTForTime(
                        time,
                        startTime,
                        bezierRightTime != null ? bezierRightTime.x + startTime : startTime,
                        bezierLeftTime != null ? bezierLeftTime.x + endTime : endTime,
                        endTime
                )),
                cubicBezier(startValue.y, p1.y, p2.y, endValue.y, solveBezierTForTime(
                        time,
                        startTime,
                        bezierRightTime != null ? bezierRightTime.y + startTime : startTime,
                        bezierLeftTime != null ? bezierLeftTime.y + endTime : endTime,
                        endTime
                )),
                cubicBezier(startValue.z, p1.z, p2.z, endValue.z, solveBezierTForTime(
                        time,
                        startTime,
                        bezierRightTime != null ? bezierRightTime.z + startTime : startTime,
                        bezierLeftTime != null ? bezierLeftTime.z + endTime : endTime,
                        endTime
                ))
        );
    }

    /**
     * Interpolates four vectors as catmull-rom.
     * @param p0 p0
     * @param p1 p1
     * @param p2 p2
     * @param p3 p3
     * @param t alpha
     * @return interpolated vector
     */
    public static @NotNull Vector3f catmull_rom(@NotNull Vector3f p0, @NotNull Vector3f p1, @NotNull Vector3f p2, @NotNull Vector3f p3, float t) {
        var t2 = t * t;
        var t3 = t2 * t;
        return new Vector3f(
                fma(t3, fma(-1F, p0.x, fma(3F, p1.x, fma(-3F, p2.x, p3.x))), fma(t2, fma(2F, p0.x, fma(-5F, p1.x, fma(4F, p2.x, -p3.x))), fma(t, -p0.x + p2.x, 2F * p1.x))),
                fma(t3, fma(-1F, p0.y, fma(3F, p1.y, fma(-3F, p2.y, p3.y))), fma(t2, fma(2F, p0.y, fma(-5F, p1.y, fma(4F, p2.y, -p3.y))), fma(t, -p0.y + p2.y, 2F * p1.y))),
                fma(t3, fma(-1F, p0.z, fma(3F, p1.z, fma(-3F, p2.z, p3.z))), fma(t2, fma(2F, p0.z, fma(-5F, p1.z, fma(4F, p2.z, -p3.z))), fma(t, -p0.z + p2.z, 2F * p1.z)))
        ).mul(0.5F);
    }

    /**
     * Vector point builder
     */
    @FunctionalInterface
    public interface VectorPointBuilder {
        /**
         * Interpolates vector from given float
         * @param nextFloat next float value
         * @return interpolated vector
         */
        @NotNull VectorResult build(float nextFloat);
    }

    /**
     * Vector result
     * @param vector vector
     * @param skipInterpolation skip interpolation
     */
    public record VectorResult(@NotNull Vector3f vector, boolean skipInterpolation) {
        /**
         * Vector result
         * @param vector vector
         */
        public VectorResult(@NotNull Vector3f vector) {
            this(vector, false);
        }
    }
}
