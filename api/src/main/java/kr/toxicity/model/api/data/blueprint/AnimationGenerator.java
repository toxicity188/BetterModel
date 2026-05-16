/*
 * This source file is part of BetterModel.
 * Copyright (c) 2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.data.blueprint;

import it.unimi.dsi.fastutil.floats.*;
import kr.toxicity.model.api.animation.VectorPoint;
import kr.toxicity.model.api.bone.BoneName;
import kr.toxicity.model.api.util.InterpolationUtil;
import kr.toxicity.model.api.util.MathUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static kr.toxicity.model.api.util.CollectionUtil.*;

/**
 * Generates animation data by interpolating keyframes and calculating bone movements.
 * <p>
 * This class processes raw animation points and generates smooth transitions for position, rotation, and scale.
 * It handles the creation of intermediate frames to ensure fluid motion, especially for rotations.
 * </p>
 *
 * @since 1.15.2
 */
@ApiStatus.Internal
public final class AnimationGenerator {

    private final List<AnimationTree> trees;

    /**
     * Creates a map of blueprint animators from the provided animation data.
     * <p>
     * This method calculates all necessary interpolation frames and builds the final animation structures for each bone.
     * </p>
     *
     * @param length the total length of the animation in seconds
     * @param children the list of root blueprint elements (bones)
     * @param pointMap a map containing raw animation data for each bone
     * @return a map of generated blueprint animators keyed by bone name
     * @since 1.15.2
     */
    public static @NotNull Map<BoneName, BlueprintAnimator> generate(
        float length,
        @NotNull List<BlueprintElement> children,
        @NotNull Map<BoneName, BlueprintAnimator.AnimatorData> pointMap
    ) {
        var floatSet = mapFloat(pointMap.values()
            .stream()
            .flatMap(BlueprintAnimator.AnimatorData::allPoints), VectorPoint::time, () -> new FloatAVLTreeSet(MathUtil.FRAME_COMPARATOR));
        floatSet.add(0F);
        floatSet.add(length);
        var generator = new AnimationGenerator(g -> pointMap.get(g.name()), children);
        generator.interpolateRotation(floatSet);
        generator.interpolateStep(floatSet);
        InterpolationUtil.insertLerpFrame(floatSet);
        return associate(
            pointMap.values()
                .stream()
                .parallel()
                .map(v -> new BlueprintAnimator(
                    v.name(),
                    InterpolationUtil.buildAnimation(
                        v.position(),
                        v.rotation(),
                        v.scale(),
                        v.rotationGlobal(),
                        floatSet
                    ))
                ),
            BlueprintAnimator::name
        );
    }

    private AnimationGenerator(
        @NotNull Function<BlueprintElement.Group, BlueprintAnimator.AnimatorData> function,
        @NotNull List<BlueprintElement> children
    ) {
        trees = filterIsInstance(children, BlueprintElement.Group.class)
            .map(g -> new AnimationTree(null, g, function))
            .flatMap(AnimationTree::flatten)
            .toList();
    }

    /**
     * Inserts additional keyframes to smooth out large rotations.
     * <p>
     * This ensures that rotations larger than 90 degrees between frames are broken down into smaller steps.
     * </p>
     *
     * @param floats the set of keyframe times to update
     * @since 1.15.2
     */
    public void interpolateRotation(@NotNull FloatSortedSet floats) {
        var list = new FloatArrayList(floats);
        var map = associate(
            trees.stream()
                .parallel()
                .map(t -> {
                    var d = t.data;
                    if (d == null) return null;
                    var rot = d.rotation();
                    if (rot.size() < 2) return null;
                    var interpolator = InterpolationUtil.interpolatorFor(rot);
                    return new RotationVector(
                        t,
                        list.doubleStream()
                            .mapToObj(value -> interpolator.build((float) value).vector())
                            .toList()
                    );
                })
                .filter(Objects::nonNull),
            v -> v.tree,
            v -> v.vectors
        );
        if (map.isEmpty()) return;
        IntStream.range(1, list.size())
            .parallel()
            .forEach(i -> {
                var cache = new IdentityHashMap<AnimationTree, Vector3f>(trees.size());
                for (AnimationTree t : trees) {
                    Vector3f delta, parent;
                    var getVec = map.get(t);
                    delta = getVec != null ? getVec.get(i).sub(getVec.get(i - 1), new Vector3f()) : new Vector3f();
                    cache.put(t, t.parent != null && (parent = cache.get(t.parent)) != null ? delta.add(parent) : delta);
                }
                var length = (float) Math.ceil(cache.values().stream().mapToDouble(Vector3f::length).max().orElse(0.0) / 90.0);
                if (length < 2F) return;
                var previous = list.getFloat(i - 1);
                var next = list.getFloat(i);
                var interpolateTime = Math.max(
                    (next - previous) / length,
                    MathUtil.MINECRAFT_TICK_SECONDS
                );
                synchronized (floats) {
                    for (float f = 1; f < length; f++) {
                        var addTime = MathUtil.fma(f, interpolateTime, previous);
                        if (next - addTime < MathUtil.MINECRAFT_TICK_SECONDS + MathUtil.FRAME_EPSILON) break;
                        floats.add(addTime);
                    }
                }
            });
    }

    private record RotationVector(@NotNull AnimationTree tree, @NotNull List<Vector3f> vectors) {}

    /**
     * Inserts keyframes for step interpolation (non-continuous transitions).
     *
     * @param floats the set of keyframe times to update
     * @since 1.15.2
     */
    public void interpolateStep(@NotNull FloatSortedSet floats) {
        trees.stream()
            .map(tree -> tree.data)
            .filter(Objects::nonNull)
            .forEach(data -> {
                interpolateStep(floats, data.position());
                interpolateStep(floats, data.rotation());
                interpolateStep(floats, data.scale());
            });
    }

    private void interpolateStep(@NotNull FloatSortedSet floats, @NotNull List<VectorPoint> points) {
        if (points.size() < 2) return;
        for (int i = 1; i < points.size(); i++) {
            var before = points.get(i - 1);
            if (before.isContinuous()) continue;
            var time = points.get(i).time() - MathUtil.MINECRAFT_TICK_SECONDS;
            if (time < 0 || time - before.time() < 0) continue;
            floats.add(time);
        }
    }

    private static final class AnimationTree {
        private final AnimationTree parent;
        private final List<AnimationTree> children;
        private final BlueprintAnimator.AnimatorData data;

        AnimationTree(
            @Nullable AnimationTree parent,
            @NotNull BlueprintElement.Group group,
            @NotNull Function<BlueprintElement.Group, BlueprintAnimator.AnimatorData> function
        ) {
            this.parent = parent;
            this.data = function.apply(group);
            children = filterIsInstance(group.children(), BlueprintElement.Group.class)
                .map(g -> new AnimationTree(this, g, function))
                .toList();
        }

        @NotNull
        Stream<AnimationTree> flatten() {
            return children.isEmpty() ? Stream.of(this) : Stream.concat(
                Stream.of(this),
                children.stream().flatMap(AnimationTree::flatten)
            );
        }
    }
}
