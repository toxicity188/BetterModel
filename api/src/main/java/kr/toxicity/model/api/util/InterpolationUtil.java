package kr.toxicity.model.api.util;

import it.unimi.dsi.fastutil.floats.FloatAVLTreeSet;
import it.unimi.dsi.fastutil.floats.FloatCollection;
import it.unimi.dsi.fastutil.floats.FloatComparators;
import it.unimi.dsi.fastutil.floats.FloatSet;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationPoint;
import kr.toxicity.model.api.animation.VectorPoint;
import kr.toxicity.model.api.tracker.Tracker;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static kr.toxicity.model.api.util.MathUtil.FRAME_EPSILON;
import static kr.toxicity.model.api.util.MathUtil.fma;

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

    private static void point(@NotNull FloatSet target, @NotNull List<VectorPoint> points) {
        for (VectorPoint point : points) {
            target.add(point.time());
        }
    }

    public static @NotNull List<AnimationPoint> putAnimationPoint(@NotNull List<AnimationPoint> animations, @NotNull FloatSet points) {
        return sum(
                animations.stream().map(AnimationPoint::position).distinct().toList(),
                animations.stream().map(AnimationPoint::rotation).distinct().toList(),
                animations.stream().map(AnimationPoint::scale).distinct().toList(),
                points
        );
    }

    public static @NotNull List<AnimationPoint> sum(float length, @NotNull List<VectorPoint> position, @NotNull List<VectorPoint> rotation, @NotNull List<VectorPoint> scale) {
        var set = new FloatAVLTreeSet(FloatComparators.NATURAL_COMPARATOR);
        set.add(0);
        set.add(length);
        point(set, position);
        point(set, scale);
        point(set, rotation);
        return sum(position, rotation, scale, set);
    }

    public static @NotNull List<AnimationPoint> sum(@NotNull List<VectorPoint> position, @NotNull List<VectorPoint> rotation, @NotNull List<VectorPoint> scale, @NotNull FloatSet points) {
        var list = new ArrayList<AnimationPoint>();
        var pp = putPoint(position, points);
        var rp = putPoint(rotation, points);
        var sp = putPoint(scale, points);
        for (int i = 0; i < pp.size(); i++) {
            list.add(new AnimationPoint(
                    pp.get(i),
                    rp.get(i),
                    sp.get(i)
            ));
        }
        return list;
    }

    public static @NotNull List<VectorPoint> putPoint(@NotNull List<VectorPoint> vectors, @NotNull FloatSet points) {
        var newVectors = new ArrayList<VectorPoint>();
        if (vectors.size() < 2) {
            var first = vectors.isEmpty() ? VectorPoint.EMPTY : vectors.getFirst();
            points.iterator().forEachRemaining(f -> newVectors.add(new VectorPoint(first.vector(), f, first.interpolation())));
            return newVectors;
        }
        var p1 = VectorPoint.EMPTY;
        var p2 = vectors.getFirst();
        var last = vectors.getLast();
        var length = last.time();
        var i = 0;
        var t = p2.time();
        for (float point : points) {
            while (i < vectors.size() - 1 && t < point) {
                p1 = p2;
                t = (p2 = vectors.get(++i)).time();
            }
            if (point > length) newVectors.add(new VectorPoint(
                    last.vector(),
                    point,
                    last.interpolation()
            ));
            else {
                newVectors.add(point == t ? vectors.get(i) : p1.interpolation().interpolate(vectors, i, point));
            }
        }
        if (t < length) newVectors.addAll(vectors.subList(i, vectors.size()));
        return newVectors;
    }

    public static void insertLerpFrame(@NotNull FloatCollection frames) {
        insertLerpFrame(frames, (float) BetterModel.config().lerpFrameTime() / 20F);
    }

    public static float roundTime(float time) {
        return (int) fma(time, FRAME_HASH_REVERT, FRAME_EPSILON) * FRAME_HASH;
    }

    public static void insertLerpFrame(@NotNull FloatCollection frames, float frame) {
        if (frame <= 0F) return;
        var first = 0F;
        var second = 0F;
        var iterator = new FloatAVLTreeSet(frames).iterator();
        while (iterator.hasNext()) {
            first = second;
            second = iterator.nextFloat();
            var max = (int) ((second - first) / frame);
            for (int i = 0; i < max; i++) {
                var add = first + frame * (i + 1);
                if (second - add < frame) continue;
                frames.add(add);
            }
        }
    }

    public static float alpha(float p0, float p1, float alpha) {
        var div = p1 - p0;
        return div == 0 ? 0 : (alpha - p0) / div;
    }

    public static @NotNull Vector3f lerp(@NotNull Vector3f p0, @NotNull Vector3f p1, float alpha) {
        return new Vector3f(
                lerp(p0.x, p1.x, alpha),
                lerp(p0.y, p1.y, alpha),
                lerp(p0.z, p1.z, alpha)
        );
    }

    public static float lerp(float p0, float p1, float alpha) {
        return fma(p1 - p0, alpha, p0);
    }

    public static float cubicBezier(float p0, float p1, float p2, float p3, float t) {
        float u = 1.0F - t;
        float uu = u * u;
        float tt = t * t;
        float uuu = uu * u;
        float utt = u * tt;
        float uut = uu * t;
        float ttt = tt * t;
        return fma(uuu, p0, fma(3.0F * uut, p1, fma(3.0F * utt, p2, ttt * p3)));
    }

    public static float derivativeBezier(float p0, float p1, float p2, float p3, float t) {
        float u = 1.0F - t;
        float uu = u * u;
        float ut = u * t;
        float tt = t * t;
        return fma(3.0F * uu, p1 - p0, fma(6.0F * ut, p2 - p1, 3.0F * tt * (p3 - p2)));
    }

    public static float solveBezierTForTime(float time, float t0, float h1, float h2, float t1) {
        float t = 0.5F;
        int maxIterations = 20;
        float epsilon = 1e-5F;
        for (int i = 0; i < maxIterations; i++) {
            float bezTime = cubicBezier(t0, h1, h2, t1, t);
            float derivative = derivativeBezier(t0, h1, h2, t1, t);
            float error = bezTime - time;
            if (Math.abs(error) < epsilon) {
                return t;
            }
            if (derivative != 0) {
                t -= error / derivative;
            }
            t = Math.clamp(t, 0F, 1F);
        }

        return t;
    }

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

    public static @NotNull Vector3f catmull_rom(@NotNull Vector3f p0, @NotNull Vector3f p1, @NotNull Vector3f p2, @NotNull Vector3f p3, float t) {
        var t2 = t * t;
        var t3 = t2 * t;
        return new Vector3f(
                fma(t3, fma(-1F, p0.x, fma(3F, p1.x, fma(-3F, p2.x, p3.x))), fma(t2, fma(2F, p0.x, fma(-5F, p1.x, fma(4F, p2.x, -p3.x))), fma(t, -p0.x + p2.x, 2F * p1.x))),
                fma(t3, fma(-1F, p0.y, fma(3F, p1.y, fma(-3F, p2.y, p3.y))), fma(t2, fma(2F, p0.y, fma(-5F, p1.y, fma(4F, p2.y, -p3.y))), fma(t, -p0.y + p2.y, 2F * p1.y))),
                fma(t3, fma(-1F, p0.z, fma(3F, p1.z, fma(-3F, p2.z, p3.z))), fma(t2, fma(2F, p0.z, fma(-5F, p1.z, fma(4F, p2.z, -p3.z))), fma(t, -p0.z + p2.z, 2F * p1.z)))
        ).mul(0.5F);
    }
}
