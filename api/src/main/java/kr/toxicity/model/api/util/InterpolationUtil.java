/*
 * This source file is part of BetterModel.
 * Copyright (c) 2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.util;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatSortedSet;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationKeyframe;
import kr.toxicity.model.api.animation.keyframe.KeyframePoint;
import kr.toxicity.model.api.animation.keyframe.KeyframeVector;
import kr.toxicity.model.api.tracker.Tracker;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.List;

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
     * Builds animation keyframe
     * @param position position point
     * @param rotation rotation point
     * @param scale scale point
     * @param rotationGlobal rotation global
     * @param points keyframe time set
     * @return animation keyframe
     */
    @NotNull
    public static AnimationKeyframe buildAnimation(
        @NotNull List<KeyframePoint> position,
        @NotNull List<KeyframePoint> rotation,
        @NotNull List<KeyframePoint> scale,
        boolean rotationGlobal,
        @NotNull FloatSortedSet points
    ) {
        var pp = interpolatorFor(position);
        var rp = interpolatorFor(rotation);
        var sp = interpolatorFor(scale);
        var keyframe = AnimationKeyframe.builder(
            points.size(),
            rotationGlobal,
            position.stream().allMatch(KeyframePoint::isConstant),
            rotation.stream().allMatch(KeyframePoint::isConstant),
            scale.stream().allMatch(KeyframePoint::isConstant)
        );
        var before = 0F;
        var iterator = points.iterator();
        while (iterator.hasNext()) {
            var f = iterator.nextFloat();
            var pr = pp.build(f);
            var rr = rp.build(f);
            var sr = sp.build(f);
            keyframe.writePosition(pr.vector);
            keyframe.writeRotation(rr.vector);
            keyframe.writeScale(sr.vector);
            keyframe.writeExtra(roundTime(f - before), pr.skipInterpolation || sr.skipInterpolation || rr.skipInterpolation);
            before = f;
        }
        return keyframe.build();
    }

    /**
     * Creates interpolator
     * @param vectors target vector list
     * @return point builder
     */
    public static @NotNull VectorPointBuilder interpolatorFor(@NotNull List<KeyframePoint> vectors) {
        var last = vectors.isEmpty() ? KeyframePoint.EMPTY : vectors.getLast();
        return vectors.size() < 2 ? f -> new VectorResult(last.vector(f)) : new VectorPointBuilder() {
            private KeyframePoint p1 = KeyframePoint.EMPTY;
            private KeyframePoint p2 = vectors.getFirst();
            private int i = 0;
            private float t = p2.time();

            @Override
            public @NotNull VectorResult build(float nextFloat) {
                while (i < vectors.size() - 1 && t < nextFloat) {
                    p1 = p2;
                    t = (p2 = vectors.get(++i)).time();
                }
                if (nextFloat > last.time()) return new VectorResult(last.vector(nextFloat), !last.isContinuous());
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
        insertLerpFrame(frames, (float) BetterModel.config().lerpFrameTime() / MINECRAFT_TICKS_PER_SECOND);
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
        var u = 1.0F - t;
        var uu = u * u;
        var tt = t * t;
        var uuu = uu * u;
        var utt = u * tt;
        var uut = uu * t;
        var ttt = tt * t;
        return fma(uuu, p0, fma(3.0F * uut, p1, fma(3.0F * utt, p2, ttt * p3)));
    }

    private static float derivativeBezier(float p1, float p2, float t) {
        var u = 1.0F - t;
        var uu = u * u;
        var ut = u * t;
        var tt = t * t;
        return fma(3.0F * uu, p1, fma(6.0F * ut, p2 - p1, 3.0F * tt * (1 - p2)));
    }

    private static float solveBezierTForTime(float time, float h1, float h2) {
        var t = 0.5F;
        var maxIterations = 20;
        for (int i = 0; i < maxIterations; i++) {
            var bezTime = cubicBezier(0, h1, h2, 1, t);
            var derivative = derivativeBezier(h1, h2, t);
            var error = bezTime - time;
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

    public static float bezier(
        float alpha,
        float start,
        float end,
        float bezierRightTime,
        float bezierRightValue,
        float bezierLeftTime,
        float bezierLeftValue
    ) {
        return cubicBezier(start, start + bezierRightValue, end + bezierLeftValue, end, solveBezierTForTime(
            alpha,
            bezierRightTime,
            1 + bezierLeftTime
        ));
    }

    public static float catmull_rom(
        float p0,
        float p1,
        float p2,
        float p3,
        float t
    ) {
        var t2 = t * t;
        var t3 = t2 * t;
        return 0.5F * fma(t3, fma(-1F, p0, fma(3F, p1, fma(-3F, p2, p3))), fma(t2, fma(2F, p0, fma(-5F, p1, fma(4F, p2, -p3))), fma(t, -p0 + p2, 2F * p1)));
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
    public record VectorResult(@NotNull KeyframeVector vector, boolean skipInterpolation) {
        /**
         * Vector result
         * @param vector vector
         */
        public VectorResult(@NotNull KeyframeVector vector) {
            this(vector, false);
        }
    }
}
