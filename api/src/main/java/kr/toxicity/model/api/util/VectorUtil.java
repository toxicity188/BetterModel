package kr.toxicity.model.api.util;

import it.unimi.dsi.fastutil.floats.*;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationPoint;
import kr.toxicity.model.api.animation.VectorPoint;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@ApiStatus.Internal
public final class VectorUtil {

    private VectorUtil() {
        throw new RuntimeException();
    }

    private static void point(@NotNull Collection<Float> target, List<VectorPoint> points) {
        for (VectorPoint point : points) {
            target.add(point.time());
        }
    }

    public static @NotNull List<AnimationPoint> putAnimationPoint(@NotNull List<AnimationPoint> animations, @NotNull FloatCollection points) {
        return sum(
                animations.stream().map(AnimationPoint::position).distinct().toList(),
                animations.stream().map(AnimationPoint::rotation).distinct().toList(),
                animations.stream().map(AnimationPoint::scale).distinct().toList(),
                points
        );
    }

    public static @NotNull List<AnimationPoint> sum(float length, @NotNull List<VectorPoint> position, @NotNull List<VectorPoint> rotation, @NotNull List<VectorPoint> scale) {
        var set = new FloatAVLTreeSet(FloatComparators.NATURAL_COMPARATOR);
        set.add(length);
        point(set, position);
        point(set, scale);
        point(set, rotation);
        insertRotationFrame(set, rotation);
        insertLerpFrame(set);
        return sum(position, rotation, scale, set);
    }

    public static @NotNull List<AnimationPoint> sum(@NotNull List<VectorPoint> position, @NotNull List<VectorPoint> rotation, @NotNull List<VectorPoint> scale, FloatCollection points) {
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

    public static @NotNull List<VectorPoint> putPoint(@NotNull List<VectorPoint> vectors, @NotNull FloatCollection points) {
        var newVectors = new ArrayList<VectorPoint>();
        if (vectors.isEmpty()) {
            points.iterator().forEachRemaining(f -> newVectors.add(new VectorPoint(new Vector3f(), f, VectorInterpolation.LINEAR)));
            return newVectors;
        }
        var last = vectors.getLast();
        var length = last.time();
        var i = 0;
        var p2 = vectors.getFirst();
        var t = p2.time();
        for (float point : points) {
            while (i < vectors.size() - 1 && t < point) {
                t = (p2 = vectors.get(++i)).time();
            }
            if (point > length) newVectors.add(new VectorPoint(
                    last.vector(),
                    point,
                    last.interpolation()
            ));
            else {
                newVectors.add(point == t ? vectors.get(i) : p2.interpolation().interpolate(vectors, i, point));
            }
        }
        if (t < length) newVectors.addAll(vectors.subList(i, vectors.size()));
        return newVectors;
    }

    public static void insertRotationFrame(@NotNull FloatSet frames, @NotNull List<VectorPoint> vectorPoints) {
        for (int i = 0; i < vectorPoints.size() - 1; i++) {
            var before = vectorPoints.get(i);
            var after = vectorPoints.get(i + 1);
            var angle = (float) Math.ceil(Math.toDegrees(MathUtil.toQuaternion(after.vector())
                    .mul(MathUtil.toQuaternion(before.vector()).invert())
                    .angle()) / 45F);
            if (angle > 1) {
                for (float t = 1; t < angle; t++) {
                    frames.add(linear(before.time(), after.time(), t / angle));
                }
            }
        }
    }

    public static void insertLerpFrame(@NotNull FloatCollection frames) {
        insertLerpFrame(frames, (float) BetterModel.plugin().configManager().lerpFrameTime() / 20F);
    }

    private static final float FRAME_HASH = 0.031F;

    public static void insertLerpFrame(@NotNull FloatCollection frames, float frame) {
        if (frame <= 0F) return;
        frame += FRAME_HASH;
        var list = new FloatArrayList(frames);
        var init = 0F;
        var initAfter = list.getFirst();
        while ((init += frame) < initAfter) {
            frames.add(init);
        }
        for (int i = 0; i < list.size() - 1; i++) {
            var before = list.getFloat(i);
            var after = list.getFloat(i + 1);
            while ((before += frame) < after) {
                frames.add(before);
            }
        }
    }

    public static float alpha(float p0, float p1, float alpha) {
        var div = p1 - p0;
        return div == 0 ? 0 : (alpha - p0) / div;
    }

    public static @NotNull Vector3f linear(@NotNull Vector3f p0, @NotNull Vector3f p1, float alpha) {
        return new Vector3f(
                linear(p0.x, p1.x, alpha),
                linear(p0.y, p1.y, alpha),
                linear(p0.z, p1.z, alpha)
        );
    }

    public static float linear(float p0, float p1, float alpha) {
        return Math.fma(p1 - p0, alpha, p0);
    }

    public static @NotNull Vector3f catmull_rom(@NotNull Vector3f p0, @NotNull Vector3f p1, @NotNull Vector3f p2, @NotNull Vector3f p3, float t) {
        var t2 = t * t;
        var t3 = t2 * t;
        return new Vector3f(
                Math.fma(t3, Math.fma(-1f, p0.x, Math.fma(3f, p1.x, Math.fma(-3f, p2.x, p3.x))), Math.fma(t2, Math.fma(2f, p0.x, Math.fma(-5f, p1.x, Math.fma(4f, p2.x, -p3.x))), Math.fma(t, -p0.x + p2.x, 2f * p1.x))),
                Math.fma(t3,Math.fma(-1f, p0.y, Math.fma(3f, p1.y, Math.fma(-3f, p2.y, p3.y))), Math.fma(t2, Math.fma(2f, p0.y, Math.fma(-5f, p1.y, Math.fma(4f, p2.y, -p3.y))), Math.fma(t, -p0.y + p2.y, 2f * p1.y))),
                Math.fma(t3, Math.fma(-1f, p0.z, Math.fma(3f, p1.z, Math.fma(-3f, p2.z, p3.z))), Math.fma(t2, Math.fma(2f, p0.z, Math.fma(-5f, p1.z, Math.fma(4f, p2.z, -p3.z))), Math.fma(t, -p0.z + p2.z, 2f * p1.z)))
        ).mul(0.5F);
    }
}
