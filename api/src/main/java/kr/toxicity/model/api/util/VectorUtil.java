package kr.toxicity.model.api.util;

import it.unimi.dsi.fastutil.floats.FloatAVLTreeSet;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationPoint;
import kr.toxicity.model.api.animation.VectorPoint;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.*;

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

    public static @NotNull List<AnimationPoint> putAnimationPoint(@NotNull List<AnimationPoint> animations, @NotNull Collection<Float> points) {
        return sum(
                animations.stream().map(AnimationPoint::position).distinct().toList(),
                animations.stream().map(AnimationPoint::rotation).distinct().toList(),
                animations.stream().map(AnimationPoint::scale).distinct().toList(),
                points
        );
    }

    public static @NotNull List<AnimationPoint> sum(float length, @NotNull List<VectorPoint> position, @NotNull List<VectorPoint> rotation, @NotNull List<VectorPoint> scale) {
        var set = new FloatAVLTreeSet();
        set.add(length);
        point(set, position);
        point(set, scale);
        point(set, rotation);
        insertRotationFrame(set, rotation);
        insertLerpFrame(set);
        return sum(position, rotation, scale, set);
    }

    public static @NotNull List<AnimationPoint> sum(@NotNull List<VectorPoint> position, @NotNull List<VectorPoint> rotation, @NotNull List<VectorPoint> scale, Collection<Float> points) {
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

    public static @NotNull List<VectorPoint> putPoint(@NotNull List<VectorPoint> vectors, @NotNull Collection<Float> points) {
        if (vectors.isEmpty()) return points.stream().map(t -> new VectorPoint(new Vector3f(), t, VectorInterpolation.LINEAR)).toList();
        var last = vectors.getLast();
        var length = last.time();
        var i = 0;
        var p2 = vectors.getFirst();
        var t = p2.time();
        var newVectors = new ArrayList<VectorPoint>();
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

    public static void insertRotationFrame(@NotNull Set<Float> frames, @NotNull List<VectorPoint> vectorPoints) {
        for (int i = 0; i < vectorPoints.size() - 1; i++) {
            var before = vectorPoints.get(i);
            var after = vectorPoints.get(i + 1);
            var angle = (float) Math.ceil(Math.toDegrees(MathUtil.toQuaternion(MathUtil.blockBenchToDisplay(after.vector()))
                    .mul(MathUtil.toQuaternion(MathUtil.blockBenchToDisplay(before.vector())).invert())
                    .angle()) / 45F);
            if (angle > 1) {
                for (float t = 1; t < angle; t++) {
                    frames.add(linear(before.time(), after.time(), t / angle));
                }
            }
        }
    }

    public static void insertLerpFrame(@NotNull Set<Float> frames) {
        insertLerpFrame(frames, (float) BetterModel.inst().configManager().lerpFrameTime() / 20F);
    }

    private static final float FRAME_HASH = 0.031F;

    public static void insertLerpFrame(@NotNull Set<Float> frames, float frame) {
        if (frame <= FRAME_HASH) return;
        frame -= FRAME_HASH;
        var list = new ArrayList<>(frames);
        var init = 0F;
        var initAfter = list.getFirst();
        while ((init += frame) < initAfter) {
            frames.add(init);
        }
        for (int i = 0; i < list.size() - 1; i++) {
            var before = list.get(i);
            var after = list.get(i + 1);
            while ((before += frame) < after) {
                frames.add(before);
            }
        }
    }

    public static float alpha(float p0, float p1, float alpha) {
        var div = p1 - p0;
        return div == 0 ? p0 : (alpha - p0) / div;
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
                0.5F * (2 * p1.x + (-p0.x + p2.x) * t + (2 * p0.x - 5 * p1.x + 4 * p2.x - p3.x) * t2 + (-p0.x + 3 * p1.x - 3 * p2.x + p3.x) * t3),
                0.5F * (2 * p1.y + (-p0.y + p2.y) * t + (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) * t2 + (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) * t3),
                0.5F * (2 * p1.z + (-p0.z + p2.z) * t + (2 * p0.z - 5 * p1.z + 4 * p2.z - p3.z) * t2 + (-p0.z + 3 * p1.z - 3 * p2.z + p3.z) * t3)
        );
    }
}
