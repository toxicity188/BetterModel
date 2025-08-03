package kr.toxicity.model.api.data.blueprint;

import it.unimi.dsi.fastutil.floats.*;
import kr.toxicity.model.api.animation.VectorPoint;
import kr.toxicity.model.api.bone.BoneName;
import kr.toxicity.model.api.util.InterpolationUtil;
import kr.toxicity.model.api.util.MathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static kr.toxicity.model.api.util.CollectionUtil.*;

public final class AnimationGenerator {

    private static final Vector3f EMPTY = new Vector3f();
    private final Map<BoneName, BlueprintAnimator.AnimatorData> pointMap;
    private final List<AnimationTree> trees;

    public static @NotNull Map<BoneName, BlueprintAnimator> createMovements(
            float length,
            @NotNull List<BlueprintChildren> children,
            @NotNull Map<BoneName, BlueprintAnimator.AnimatorData> pointMap
    ) {
        var floatSet = mapFloat(pointMap.values()
                .stream()
                .flatMap(BlueprintAnimator.AnimatorData::allPoints), VectorPoint::time, () -> new FloatAVLTreeSet(MathUtil.FRAME_COMPARATOR));
        floatSet.add(0F);
        floatSet.add(length);
        InterpolationUtil.insertLerpFrame(floatSet);
        new AnimationGenerator(pointMap, children).interpolateRotation(floatSet);
        return mapValue(pointMap, v -> new BlueprintAnimator(
                v.name(),
                InterpolationUtil.buildAnimation(
                        v.transform(),
                        v.rotation(),
                        v.scale(),
                        floatSet
                )
        ));
    }

    private AnimationGenerator(
            @NotNull Map<BoneName, BlueprintAnimator.AnimatorData> pointMap,
            @NotNull List<BlueprintChildren> children
    ) {
        this.pointMap = pointMap;
        trees = filterIsInstance(children, BlueprintChildren.BlueprintGroup.class)
                .map(g -> new AnimationTree(g, pointMap.get(g.name())))
                .flatMap(AnimationTree::flatten)
                .toList();
    }

    private float firstTime = 0F;
    private float secondTime = 0F;
    public void interpolateRotation(@NotNull FloatSortedSet floats) {
        var iterator = new FloatArrayList(floats).iterator();
        while (iterator.hasNext()) {
            firstTime = secondTime;
            secondTime = iterator.nextFloat();
            if (secondTime - firstTime <= 0) continue;
            var minus = trees.stream()
                    .mapToDouble(t -> t.tree(firstTime, secondTime, BlueprintAnimator.AnimatorData::rotation))
                    .max()
                    .orElse(0);
            var length = (float) Math.ceil(minus / 90);
            if (length < 2) continue;
            var last = firstTime;
            for (float f = 1; f < length; f++) {
                var addTime = InterpolationUtil.lerp(firstTime, secondTime, f / length);
                if (addTime - last < 0.01 || secondTime - addTime < 0.01) continue;
                floats.add(last = addTime);
            }
        }
    }

    private class AnimationTree {
        private final AnimationTree parent;
        private final List<AnimationTree> children;
        private final BlueprintAnimator.AnimatorData data;
        private int searchCache = 0;
        private final Float2ObjectMap<Vector3f> valueCache = new Float2ObjectOpenHashMap<>();

        AnimationTree(@NotNull BlueprintChildren.BlueprintGroup group, @Nullable BlueprintAnimator.AnimatorData data) {
            this(null, group, data);
        }
        AnimationTree(
                @Nullable AnimationTree parent,
                @NotNull BlueprintChildren.BlueprintGroup group,
                @Nullable BlueprintAnimator.AnimatorData data
        ) {
            this.parent = parent;
            this.data = data;
            children = filterIsInstance(group.children(), BlueprintChildren.BlueprintGroup.class)
                    .map(g -> new AnimationTree(this, g, pointMap.get(g.name())))
                    .toList();
        }

        @NotNull
        Stream<AnimationTree> flatten() {
            return Stream.concat(
                    Stream.of(this),
                    children.stream().flatMap(AnimationTree::flatten)
            );
        }

        private float tree(float first, float second, @NotNull Function<BlueprintAnimator.AnimatorData, List<VectorPoint>> mapper) {
            var value = data != null ? mapper.apply(data) : Collections.<VectorPoint>emptyList();
            return findTree(first, second, value).length();
        }

        private @NotNull Vector3f findTree(float first, float second, @NotNull List<VectorPoint> target) {
            var get = find(first, second, target);
            return parent != null ? parent.findTree(first, second, target).add(get) : get;
        }
        private @NotNull Vector3f find(float first, float second, @NotNull List<VectorPoint> target) {
            return find(second, target).sub(find(first, target), new Vector3f());
        }
        private @NotNull Vector3f find(float time, @NotNull List<VectorPoint> target) {
            return valueCache.computeIfAbsent(time, f -> {
                if (target.size() <= 1) return EMPTY;
                var i = searchCache;
                for (; i < target.size(); i++) {
                    if (target.get(i).time() >= time) break;
                }
                searchCache = i;
                if (i == 0) return EMPTY;
                if (i == target.size()) return EMPTY;
                var first = target.get(i - 1);
                var second = target.get(i);
                return second.time() == time ? second.vector(time) : InterpolationUtil.lerp(
                        first.vector(time),
                        second.vector(time),
                        InterpolationUtil.alpha(first.time(), second.time(), time)
                );
            });
        }
    }
}
