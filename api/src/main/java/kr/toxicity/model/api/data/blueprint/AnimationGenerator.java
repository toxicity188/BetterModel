/*
 * This source file is part of BetterModel.
 * Copyright (c) 2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.data.blueprint;

import it.unimi.dsi.fastutil.floats.*;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationKeyframe;
import kr.toxicity.model.api.animation.keyframe.KeyframePoint;
import kr.toxicity.model.api.bone.BoneName;
import kr.toxicity.model.api.util.InterpolationUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static kr.toxicity.model.api.util.CollectionUtil.*;
import static kr.toxicity.model.api.util.MathUtil.*;

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

    private static final float MIN_ROTATION_DOT = (float) Math.cos(Math.toRadians(179.9 / 2));
    private static final double REVERSAL_TOLERANCE = Math.toRadians(2.0 / 2);

    private final @NotNull AnimationTree[] trees;
    private final int[] parents;

    /**
     * Creates a map of blueprint animators from the provided animation data.
     * The bones share a timeline built from tick-rounded keyframe times and the supplied endpoints.
     * Configured interpolation frames are added first, followed by rotation splits and step frames.
     * <pre>{@code
     * var animators = AnimationGenerator.generate(animation.tickTime(), roots, pointsByBone);
     * }</pre>
     *
     * @param length the animation end time in seconds, normally aligned to a Minecraft tick
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
            .flatMap(BlueprintAnimator.AnimatorData::allPoints), KeyframePoint::tickTime, () -> new FloatAVLTreeSet(FRAME_COMPARATOR));
        floatSet.add(0F);
        floatSet.add(length);
        var generator = new AnimationGenerator(g -> pointMap.get(g.name()), children);
        insertLerpFrame(floatSet);
        generator.interpolateRotation(floatSet);
        generator.interpolateStep(floatSet);
        return associate(
            pointMap.values()
                .stream()
                .parallel()
                .map(v -> new BlueprintAnimator(
                    v.name(),
                    AnimationKeyframe.build(
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
        var indexer = new AtomicInteger();
        // Trees
        trees = filterIsInstance(children, BlueprintElement.Group.class)
            .map(g -> new AnimationTree(indexer, null, g, function))
            .flatMap(AnimationTree::flatten)
            .sorted(Comparator.comparingInt(t -> t.index))
            .toArray(AnimationTree[]::new);

        // Indices
        parents = new int[trees.length];
        for (AnimationTree tree : trees) {
            parents[tree.index] = tree.parent == null ? -1 : tree.parent.index;
        }
    }

    /**
     * Inserts shared keyframes where display interpolation would lose the sampled rotation path.
     * Samples intervening Minecraft ticks only for unsafe endpoint rotations or complete local Euler turns.
     * Prefers splits that preserve the path and uses fewer frames when split costs are equal.
     * <pre>{@code
     * generator.interpolateRotation(frameTimes);
     * }</pre>
     *
     * @param floats the set of keyframe times to update
     * @since 1.15.2
     */
    public void interpolateRotation(@NotNull FloatSortedSet floats) {
        if (Arrays.stream(trees).noneMatch(t -> t.data != null && !t.data.rotation().isEmpty())) return;
        if (floats.size() < 2) return;
        var times = new FloatArrayList(floats);
        var sampler = samplerFunction();
        var splits = IntStream.range(1, times.size()).parallel()
            .mapToObj(i -> rotationSplits(sampler, times.getFloat(i - 1), times.getFloat(i)))
            .toArray(FloatList[]::new);
        for (var selected : splits) floats.addAll(selected);
    }

    private static @NotNull FloatList rotationSplits(@NotNull Float2ObjectFunction<RotationFrame> sampler, float from, float to) {
        if (to - from < MINECRAFT_TICK_SECONDS * 2 - FLOAT_COMPARISON_EPSILON) return FloatList.of();
        var start = sampler.get(from);
        var end = sampler.get(to);
        var first = new RotationSample(from, start.world);
        var last = new RotationSample(to, end.world);
        if (canInterpolateRotation(List.of(first, last), 0, 1) && !start.hasFullTurnTo(end)) return FloatList.of();

        var samples = new ArrayList<RotationSample>();
        samples.add(first);
        for (long tick = (long) Math.floor((double) from * MINECRAFT_TICKS_PER_SECOND) + 1; ; tick++) {
            var time = tick * MINECRAFT_TICK_SECONDS;
            if (!frameSpacing(time, to)) break;
            if (frameSpacing(from, time) && time > samples.getLast().time) {
                samples.add(new RotationSample(time, sampler.get(time).world));
            }
        }
        if (samples.size() == 1) return FloatList.of();
        samples.add(last);
        return canInterpolateRotation(samples, 0, samples.size() - 1) ? FloatList.of() : rotationSplits(samples);
    }

    private record RotationFrame(@NotNull Vector3f[] localEuler, @NotNull Quaternionf[] world) {
        boolean hasFullTurnTo(@NotNull RotationFrame end) {
            for (int i = 0; i < localEuler.length; i++) {
                var a = localEuler[i];
                var b = end.localEuler[i];
                if (Math.abs(b.x - a.x) >= 360F || Math.abs(b.y - a.y) >= 360F || Math.abs(b.z - a.z) >= 360F) return true;
            }
            return false;
        }
    }

    private record RotationSample(float time, @NotNull Quaternionf[] world) {}


    private static boolean frameSpacing(float start, float end) {
        return end - start + FLOAT_COMPARISON_EPSILON >= MINECRAFT_TICK_SECONDS;
    }

    private @NotNull Float2ObjectFunction<RotationFrame> samplerFunction() {
        var functions = Arrays.stream(trees)
            .map(tree -> {
                if (tree.data == null) return (SamplerFunction) _ -> tree.boneRotation;
                var interpolator = InterpolationUtil.interpolatorFor(tree.data.rotation());
                return (SamplerFunction) f -> interpolator.build(f).vector().eval().add(tree.boneRotation);
            })
            .toArray(SamplerFunction[]::new);
        var memoize = new ConcurrentHashMap<Float, RotationFrame>();
        return time -> memoize.computeIfAbsent(time, key -> {
            var local = new Vector3f[trees.length];
            var world = new Quaternionf[trees.length];
            for (int i = 0; i < trees.length; i++) {
                local[i] = functions[i].get(key.floatValue());
                var value = toQuaternion(local[i]);
                if (inheritsRotation(i)) world[parents[i]].mul(value, value);
                world[i] = value;
            }
            return new RotationFrame(local, world);
        });
    }

    private interface SamplerFunction extends Float2ObjectFunction<Vector3f> {}

    private boolean inheritsRotation(int bone) {
        var data = trees[bone].data;
        return parents[bone] >= 0 && (data == null || !data.rotationGlobal());
    }

    private static boolean canInterpolateRotation(@NotNull List<RotationSample> samples, int from, int to) {
        var start = samples.get(from);
        var end = samples.get(to);
        for (int i = 0; i < start.world.length; i++) {
            var a = start.world[i];
            var b = end.world[i];
            var dot = quaternionDot(a, b);
            // Keep the sign of the continuous lift. Minecraft SLERP flips a negative endpoint dot.
            if (!(dot > MIN_ROTATION_DOT)) return false;
            var minFromStart = 1F;
            var maxToEnd = Math.min(1F, dot);
            for (int j = from + 1; j <= to; j++) {
                var q = samples.get(j).world[i];
                var fromStart = Math.min(1F, j == to ? dot : quaternionDot(a, q));
                var toEnd = Math.min(1F, quaternionDot(b, q));
                if (!(fromStart > MIN_ROTATION_DOT && toEnd > MIN_ROTATION_DOT)) return false;
                // A single SLERP can only move away from its start and toward its end.
                // acos is decreasing. Only a reversal needs the angular tolerance check.
                if (fromStart > minFromStart && Math.acos(fromStart) + REVERSAL_TOLERANCE < Math.acos(minFromStart)) return false;
                if (toEnd < maxToEnd && Math.acos(toEnd) > Math.acos(maxToEnd) + REVERSAL_TOLERANCE) return false;
                minFromStart = Math.min(minFromStart, fromStart);
                maxToEnd = Math.max(maxToEnd, toEnd);
            }
        }
        return true;
    }

    private static float quaternionDot(@NotNull Quaternionf a, @NotNull Quaternionf b) {
        return a.x * b.x + a.y * b.y + a.z * b.z + a.w * b.w;
    }

    private static @NotNull FloatList rotationSplits(@NotNull List<RotationSample> samples) {
        var size = samples.size();
        var next = new int[size];
        var segments = new int[size];
        var unsafeTime = new double[size];

        for (int i = size - 2; i >= 0; i--) {
            var start = samples.get(i).time;
            var last = samples.getLast().time;
            if (i != 0 && !frameSpacing(start, last)) continue;

            unsafeTime[i] = Double.POSITIVE_INFINITY;

            for (int j = size - 1; j > i; j--) {
                var end = samples.get(j).time;
                if (!frameSpacing(start, end)) break;
                if (j != size - 1 && !frameSpacing(end, last)) continue;

                var count = segments[j] + 1;
                if (!isBetterSplit(unsafeTime[j], count, unsafeTime[i], segments[i])) continue;

                // The caller already rejected the full interval.
                var safe = (i != 0 || j != size - 1) && canInterpolateRotation(samples, i, j);
                var cost = unsafeTime[j] + (safe ? 0D : (double) end - start);

                if (!isBetterSplit(cost, count, unsafeTime[i], segments[i])) continue;

                unsafeTime[i] = cost;
                segments[i] = count;
                next[i] = j;

                // The direct edge was tried first. No remaining split can beat two safe segments.
                if (cost == 0 && count <= 2) break;
            }
        }

        return splitTimes(samples, next);
    }

    private static boolean isBetterSplit(double cost, int count, double currentCost, int currentCount) {
        return cost < currentCost || cost == currentCost && count < currentCount;
    }

    private static @NotNull FloatList splitTimes(@NotNull List<RotationSample> samples, int[] next) {
        var times = new FloatArrayList();
        var last = samples.size() - 1;
        for (int i = next[0]; i > 0 && i < last; i = next[i]) {
            times.add(samples.get(i).time);
        }
        return times;
    }

    /**
     * Inserts a frame one Minecraft tick before the next tick-rounded keyframe on discontinuous channels.
     * The frame must be at or after both zero and the preceding tick-rounded keyframe.
     * <pre>{@code
     * generator.interpolateStep(frameTimes);
     * }</pre>
     *
     * @param floats the set of keyframe times to update
     * @since 1.15.2
     */
    public void interpolateStep(@NotNull FloatSortedSet floats) {
        Arrays.stream(trees)
            .map(tree -> tree.data)
            .filter(Objects::nonNull)
            .forEach(data -> {
                interpolateStep(floats, data.position());
                interpolateStep(floats, data.rotation());
                interpolateStep(floats, data.scale());
            });
    }

    private void interpolateStep(@NotNull FloatSortedSet floats, @NotNull List<KeyframePoint> points) {
        if (points.size() < 2) return;
        for (int i = 1; i < points.size(); i++) {
            var before = points.get(i - 1);
            if (before.isContinuous()) continue;
            var time = points.get(i).tickTime() - MINECRAFT_TICK_SECONDS;
            if (time < 0 || time - before.tickTime() < 0) continue;
            floats.add(time);
        }
    }

    private static void insertLerpFrame(@NotNull FloatSortedSet frames) {
        insertLerpFrame(frames, (float) BetterModel.config().lerpFrameTime() / MINECRAFT_TICKS_PER_SECOND);
    }

    private static void insertLerpFrame(@NotNull FloatSortedSet frames, float frame) {
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

    private static final class AnimationTree {

        private final int index;

        private final @Nullable AnimationTree parent;
        private final @NotNull AnimationTree[] children;
        private final @Nullable BlueprintAnimator.AnimatorData data;
        private final @NotNull Vector3f boneRotation;

        AnimationTree(
            @NotNull AtomicInteger indexer,
            @Nullable AnimationTree parent,
            @NotNull BlueprintElement.Group group,
            @NotNull Function<BlueprintElement.Group, BlueprintAnimator.AnimatorData> function
        ) {
            this.index = indexer.getAndIncrement();
            this.parent = parent;
            this.data = function.apply(group);
            this.boneRotation = group.rotation().toVector();
            children = filterIsInstance(group.children(), BlueprintElement.Group.class)
                .map(g -> new AnimationTree(indexer, this, g, function))
                .toArray(AnimationTree[]::new);
        }

        @NotNull
        Stream<AnimationTree> flatten() {
            return children.length == 0 ? Stream.of(this) : Stream.concat(
                Stream.of(this),
                Arrays.stream(children).flatMap(AnimationTree::flatten)
            );
        }
    }
}
