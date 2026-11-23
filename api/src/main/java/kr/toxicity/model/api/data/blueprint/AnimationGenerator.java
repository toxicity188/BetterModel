/*
 * This source file is part of BetterModel.
 * Copyright (c) 2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.data.blueprint;

import it.unimi.dsi.fastutil.floats.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationKeyframe;
import kr.toxicity.model.api.animation.keyframe.KeyframePoint;
import kr.toxicity.model.api.bone.BoneName;
import kr.toxicity.model.api.util.InterpolationUtil;
import kr.toxicity.model.api.util.MathUtil;
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
            .flatMap(BlueprintAnimator.AnimatorData::allPoints), KeyframePoint::tickTime, () -> new FloatAVLTreeSet(MathUtil.FRAME_COMPARATOR));
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
     * Samples intervening Minecraft ticks and checks the resulting world-space quaternion path.
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
        var timeline = new FloatArrayList();
        var boundaries = new int[floats.size()];
        var iterator = floats.iterator();
        var start = iterator.nextFloat();
        timeline.add(start);
        long tick = (long) Math.floor((double) start * MINECRAFT_TICKS_PER_SECOND) + 1;
        for (int i = 1; iterator.hasNext(); i++) {
            var end = iterator.nextFloat();
            for (; tick * MINECRAFT_TICK_SECONDS < end; tick++) {
                var time = tick * MINECRAFT_TICK_SECONDS;
                if (time > timeline.getFloat(timeline.size() - 1)) {
                    timeline.add(time);
                }
            }
            boundaries[i] = timeline.size();
            timeline.add(end);
        }
        if (timeline.size() == boundaries.length) return;

        var frames = sampleEuler(timeline);
        var splits = IntStream.range(1, boundaries.length).parallel()
            .mapToObj(i -> rotationSplits(frames, boundaries[i - 1], boundaries[i]))
            .toArray(FloatList[]::new);
        for (var times : splits) floats.addAll(times);
    }

    private @NotNull FloatList rotationSplits(@NotNull EulerFrames frames, int from, int to) {
        var first = from + 1;
        var end = to;
        while (first < end && !frameSpacing(frames.time(from), frames.time(first))) first++;
        while (first < end && !frameSpacing(frames.time(end - 1), frames.time(to))) end--;
        if (first == end) return FloatList.of();

        var samples = new ArrayList<RotationSample>(end - first + 2);
        samples.add(frames.rotationSample(from));
        for (int j = first; j < end; j++) samples.add(frames.rotationSample(j));
        samples.add(frames.rotationSample(to));
        return canInterpolateRotation(samples, 0, samples.size() - 1) ? FloatList.of() : rotationSplits(samples);
    }


    private record EulerFrames(
        @NotNull FloatArrayList times,
        @NotNull Int2ObjectFunction<Quaternionf[]> function
    ) {
        float time(int index) {
            return times.getFloat(index);
        }

        @NotNull Quaternionf[] rotation(int time) {
            return function.apply(time);
        }

        @NotNull RotationSample rotationSample(int time) {
            return new RotationSample(time(time), rotation(time));
        }
    }

    private record RotationSample(float time, @NotNull Quaternionf[] world) {}


    private static boolean frameSpacing(float start, float end) {
        return end - start + MathUtil.FLOAT_COMPARISON_EPSILON >= MathUtil.MINECRAFT_TICK_SECONDS
            && end - start > MathUtil.FRAME_EPSILON;
    }

    private @NotNull EulerFrames sampleEuler(@NotNull FloatArrayList times) {
        return new EulerFrames(times, samplerFunction(times));
    }

    private @NotNull Int2ObjectFunction<Quaternionf[]> samplerFunction(@NotNull FloatArrayList times) {
        var functions = Arrays.stream(trees)
            .map(tree -> {
                var data = tree.data;
                if (data == null) {
                    var value = MathUtil.toQuaternion(tree.boneRotation);
                    return (SamplerFunction) _ -> value.get(new Quaternionf());
                }
                var interpolator = InterpolationUtil.interpolatorFor(data.rotation());
                return (SamplerFunction) f -> MathUtil.toQuaternion(interpolator.build(f).vector().eval().add(tree.boneRotation));
            })
            .toArray(SamplerFunction[]::new);
        var memoize = new ConcurrentHashMap<Integer, Quaternionf[]>();
        return k -> memoize.computeIfAbsent(k, key -> {
            var array = new Quaternionf[trees.length];
            var i = 0;
            var time = times.getFloat(key);
            for (SamplerFunction function : functions) {
                var value = function.get(time);
                if (inheritsRotation(i)) array[parents[i]].mul(value, value);
                array[i++] = value;
            }
            return array;
        });
    }

    private interface SamplerFunction extends Float2ObjectFunction<Quaternionf> {}

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
            var time = points.get(i).tickTime() - MathUtil.MINECRAFT_TICK_SECONDS;
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
