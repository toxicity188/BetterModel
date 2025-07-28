package kr.toxicity.model.api.util;

import it.unimi.dsi.fastutil.floats.*;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationMovement;
import kr.toxicity.model.api.animation.VectorPoint;
import kr.toxicity.model.api.tracker.Tracker;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static kr.toxicity.model.api.util.MathUtil.*;
import static kr.toxicity.model.api.util.FunctionUtil.takeIf;

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

    public static @NotNull List<AnimationMovement> buildAnimation(@NotNull List<VectorPoint> position, @NotNull List<VectorPoint> rotation, @NotNull List<VectorPoint> scale, @NotNull FloatSortedSet points) {
        var pp = interpolatorFor(position);
        var sp = interpolatorFor(scale);
        var rp = interpolatorFor(rotation);
        var list = new ArrayList<AnimationMovement>(points.size());
        var before = 0F;
        var iterator = points.iterator();
        while (iterator.hasNext()) {
            var f = iterator.nextFloat();
            list.add(new AnimationMovement(
                    roundTime(f - before),
                    takeIf(pp.build(f).vector(), MathUtil::isNotZero),
                    takeIf(sp.build(f).vector(), MathUtil::isNotZero),
                    takeIf(rp.build(f).vector(), MathUtil::isNotZero)
            ));
            before = f;
        }
        return list;
    }

    public static @NotNull VectorPointBuilder interpolatorFor(@NotNull List<VectorPoint> vectors) {
        if (vectors.size() < 2) {
            var first = vectors.isEmpty() ? VectorPoint.EMPTY : vectors.getFirst();
            return f -> new VectorPoint(first.vector(), f, first.interpolation());
        }
        var last = vectors.getLast();
        return new VectorPointBuilder() {
            private VectorPoint p1 = VectorPoint.EMPTY;
            private VectorPoint p2 = vectors.getFirst();
            private int i = 0;
            private float t = p2.time();

            @Override
            public @NotNull VectorPoint build(float nextFloat) {
                while (i < vectors.size() - 1 && t < nextFloat) {
                    p1 = p2;
                    t = (p2 = vectors.get(++i)).time();
                }
                if (nextFloat > last.time()) return new VectorPoint(
                        last.vector(),
                        nextFloat,
                        last.interpolation()
                );
                else {
                    return nextFloat == t ? vectors.get(i) : p1.interpolation().interpolate(vectors, i, nextFloat);
                }
            }
        };
    }

    public static void insertLerpFrame(@NotNull FloatSortedSet frames) {
        insertLerpFrame(frames, (float) BetterModel.config().lerpFrameTime() / 20F);
    }

    public static float roundTime(float time) {
        return (int) fma(time, FRAME_HASH_REVERT, FRAME_EPSILON) * FRAME_HASH;
    }

    public static void insertLerpFrame(@NotNull FloatSortedSet frames, float frame) {
        if (frame <= 0F) return;
        var first = 0F;
        var second = 0F;
        var iterator = new FloatAVLTreeSet(frames).iterator();
        while (iterator.hasNext()) {
            first = second;
            second = iterator.nextFloat();
            var max = (int) ((second - first) / frame);
            for (int i = 0; i < max; i++) {
                var add = fma(frame, i + 1, first);
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

    @FunctionalInterface
    public interface VectorPointBuilder {
        @NotNull VectorPoint build(float nextFloat);
    }
}
