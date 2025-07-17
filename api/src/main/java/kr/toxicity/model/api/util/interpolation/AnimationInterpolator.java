package kr.toxicity.model.api.util.interpolation;

import it.unimi.dsi.fastutil.floats.FloatAVLTreeSet;
import it.unimi.dsi.fastutil.floats.FloatSet;
import kr.toxicity.model.api.animation.AnimationPoint;
import kr.toxicity.model.api.animation.VectorPoint;
import kr.toxicity.model.api.bone.BoneName;
import kr.toxicity.model.api.data.blueprint.BlueprintChildren;
import kr.toxicity.model.api.util.InterpolationUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static kr.toxicity.model.api.util.CollectionUtil.*;

public final class AnimationInterpolator {

    private final Map<BoneName, List<AnimationPoint>> pointMap;
    private final List<AnimationTree> trees;

    public static @NotNull FloatSet interpolate(
            @NotNull Map<BoneName, List<AnimationPoint>> pointMap,
            @NotNull List<BlueprintChildren> children
    ) {
        var floatSet = mapFloat(pointMap.values()
                .stream()
                .flatMap(Collection::stream), p -> p.position().time(), FloatAVLTreeSet::new);
        new AnimationInterpolator(pointMap, children).interpolateRotation(floatSet);
        InterpolationUtil.insertLerpFrame(floatSet);
        return floatSet;
    }

    private AnimationInterpolator(
            @NotNull Map<BoneName, List<AnimationPoint>> pointMap,
            @NotNull List<BlueprintChildren> children
    ) {
        this.pointMap = pointMap;
        trees = filterIsInstance(children, BlueprintChildren.BlueprintGroup.class)
                .map(g -> {
                    var get = pointMap.get(g.name());
                    return new AnimationTree(g, get != null ? get : Collections.emptyList());
                })
                .flatMap(AnimationTree::flatten)
                .toList();
    }

    private float firstTime = 0F;
    private float secondTime = 0F;
    public void interpolateRotation(@NotNull FloatSet floats) {
        for (float v : new FloatAVLTreeSet(floats)) {
            firstTime = secondTime;
            secondTime = v;
            if (secondTime - firstTime <= 0) continue;
            var minus = trees.stream()
                    .mapToDouble(t -> t.addTree(firstTime, secondTime, AnimationPoint::rotation))
                    .max()
                    .orElse(0);
            var max = trees.stream()
                    .mapToDouble(t -> t.maxTree(firstTime, secondTime, AnimationPoint::rotation))
                    .max()
                    .orElse(0);
            var length = (float) Math.ceil(Math.max(minus / 90F, max / 45F));
            if (length < 2) continue;
            var last = firstTime;
            for (float f = 1; f < length; f++) {
                var addTime = InterpolationUtil.roundTime(InterpolationUtil.lerp(firstTime, secondTime, f / length));
                if (addTime - last < 0.05 || secondTime - addTime < 0.05) continue;
                floats.add(last = addTime);
            }
        }
    }

    private class AnimationTree {
        private final AnimationTree parent;
        private final List<AnimationTree> children;
        private final List<AnimationPoint> points;

        AnimationTree(@NotNull BlueprintChildren.BlueprintGroup group, @NotNull List<AnimationPoint> points) {
            this(null, group, points);
        }
        AnimationTree(
                @Nullable AnimationTree parent,
                @NotNull BlueprintChildren.BlueprintGroup group,
                @NotNull List<AnimationPoint> points
        ) {
            this.parent = parent;
            this.points = points;
            children = filterIsInstance(group.children(), BlueprintChildren.BlueprintGroup.class)
                    .map(g -> {
                        var get = pointMap.get(g.name());
                        return new AnimationTree(this, g, get != null ? get : Collections.emptyList());
                    })
                    .toList();
        }

        @NotNull
        Stream<AnimationTree> flatten() {
            return children.isEmpty() ? Stream.of(this) : children
                    .stream()
                    .flatMap(AnimationTree::flatten);
        }

        private float addTree(float first, float second, @NotNull Function<AnimationPoint, VectorPoint> mapper) {
            return max(findTree(first, second, mapper));
        }
        private float maxTree(float first, float second, @NotNull Function<AnimationPoint, VectorPoint> mapper) {
            var get = max(find(first, second, mapper));
            return parent != null ? Math.max(parent.maxTree(first, second, mapper), get) : get;
        }

        private @NotNull Vector3f findTree(float first, float second, @NotNull Function<AnimationPoint, VectorPoint> mapper) {
            var get = find(first, second, mapper);
            return parent != null ? parent.findTree(first, second, mapper).add(get) : get;
        }
        private @NotNull Vector3f find(float first, float second, @NotNull Function<AnimationPoint, VectorPoint> mapper) {
            return find(second, mapper).sub(find(first, mapper));
        }
        private @NotNull Vector3f find(float time, @NotNull Function<AnimationPoint, VectorPoint> mapper) {
            if (points.isEmpty()) return new Vector3f();
            if (points.size() == 1) return new Vector3f();
            var i = 0;
            for (AnimationPoint point : points) {
                if (mapper.apply(point).time() >= time) break;
                i++;
            }
            if (i == 0) return new Vector3f();
            if (i == points.size()) return new Vector3f();
            var first = mapper.apply(points.get(i - 1));
            var second = mapper.apply(points.get(i));
            return second.time() == time ? second.vector().get(new Vector3f()) : InterpolationUtil.lerp(
                    first.vector(),
                    second.vector(),
                    InterpolationUtil.alpha(first.time(), second.time(), time)
            );
        }
    }

    private static float max(@NotNull Vector3f vector3f) {
        //return vector3f.length();
        return Math.max(Math.abs(vector3f.x), Math.max(Math.abs(vector3f.y), Math.abs(vector3f.z)));
    }
}
