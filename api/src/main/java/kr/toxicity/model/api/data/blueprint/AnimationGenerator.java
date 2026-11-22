/*
 * This source file is part of BetterModel.
 * Copyright (c) 2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.data.blueprint;

import it.unimi.dsi.fastutil.floats.FloatAVLTreeSet;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatSortedSet;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
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
import java.util.function.Function;
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
        // Trees
        trees = filterIsInstance(children, BlueprintElement.Group.class)
            .map(g -> new AnimationTree(null, g, function))
            .flatMap(AnimationTree::flatten)
            .toArray(AnimationTree[]::new);

        // Indices
        parents = new int[trees.length];
        setupIndices();
    }

    private void setupIndices() {
        var indices = new Reference2IntOpenHashMap<AnimationTree>(trees.length);
        indices.defaultReturnValue(-1);
        for (int i = 0; i < trees.length; i++) {
            parents[i] = indices.getInt(trees[i].parent);
            indices.put(trees[i], i);
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
        var frames = sampleEuler(new FloatArrayList(floats));
        var candidates = rotationCandidates(frames);
        if (candidates.times().isEmpty()) return;

        var extraFrames = sampleEuler(candidates.times());
        for (int i = 1; i < frames.size(); i++) {
            if (candidates.from(i) == candidates.to(i)) continue;
            var samples = rotationSamples(i, frames, candidates, extraFrames);
            if (canInterpolateRotation(samples, 0, samples.size() - 1)) continue;
            addRotationSplits(samples, floats);
        }
    }

    private record EulerFrames(
        @NotNull FloatArrayList times,
        @NotNull Vector3f[][] rotations
    ) {
        int size() {
            return times.size();
        }

        float time(int index) {
            return times.getFloat(index);
        }

        @NotNull Vector3f[] rotation(int index) {
            return rotations[index];
        }
    }

    private record RotationCandidates(@NotNull FloatArrayList times, int[] offsets) {
        int from(int interval) {
            return offsets[interval - 1];
        }

        int to(int interval) {
            return offsets[interval];
        }
    }

    private record RotationSample(float time, @NotNull Quaternionf[] world) {}

    /**
     * Collects candidate ticks that leave room for at least one tick on either side.
     *
     * @param frames the existing sample times and rotations
     * @return candidate times and their offsets for each interval
     */
    private @NotNull RotationCandidates rotationCandidates(@NotNull EulerFrames frames) {
        var candidates = new FloatArrayList();
        var offsets = new int[frames.size()];
        for (int i = 1; i < frames.size(); i++) {
            var start = frames.time(i - 1);
            var end = frames.time(i);
            // Check the actual gap with frameSpacing to tolerate float error at tick boundaries.
            for (long tick = Math.round((double) start * MathUtil.MINECRAFT_TICKS_PER_SECOND) + 1; ; tick++) {
                var time = tick * MathUtil.MINECRAFT_TICK_SECONDS;
                if (!frameSpacing(time, end)) break;
                if (frameSpacing(start, time) && (candidates.isEmpty() || time > candidates.getFloat(candidates.size() - 1))) {
                    candidates.add(time);
                }
            }
            offsets[i] = candidates.size();
        }
        return new RotationCandidates(candidates, offsets);
    }

    private @NotNull List<RotationSample> rotationSamples(
        int interval,
        @NotNull EulerFrames frames,
        @NotNull RotationCandidates candidates,
        @NotNull EulerFrames extraFrames
    ) {
        var from = candidates.from(interval);
        var to = candidates.to(interval);
        var samples = new ArrayList<RotationSample>(to - from + 2);
        samples.add(rotationSample(frames.time(interval - 1), frames.rotation(interval - 1)));
        for (int j = from; j < to; j++) {
            samples.add(rotationSample(extraFrames.time(j), extraFrames.rotation(j)));
        }
        samples.add(rotationSample(frames.time(interval), frames.rotation(interval)));
        return samples;
    }

    private @NotNull RotationSample rotationSample(float time, @NotNull Vector3f[] euler) {
        var world = new Quaternionf[trees.length];
        for (int i = 0; i < trees.length; i++) {
            var local = MathUtil.toQuaternion(euler[i]);
            if (inheritsRotation(i)) world[parents[i]].mul(local, local);
            world[i] = local.normalize();
        }
        return new RotationSample(time, world);
    }

    private static boolean frameSpacing(float start, float end) {
        return end - start + MathUtil.FLOAT_COMPARISON_EPSILON >= MathUtil.MINECRAFT_TICK_SECONDS
            && end - start > MathUtil.FRAME_EPSILON;
    }

    private @NotNull EulerFrames sampleEuler(@NotNull FloatArrayList times) {
        var rotations = new Vector3f[times.size()][trees.length];
        for (int bone = 0; bone < trees.length; bone++) {
            var tree = trees[bone];
            var points = tree.data == null ? List.<KeyframePoint>of() : tree.data.rotation();
            if (points.isEmpty()) {
                for (var rotation : rotations) rotation[bone] = tree.boneRotation;
                continue;
            }
            var interpolator = InterpolationUtil.interpolatorFor(points);
            for (int i = 0; i < times.size(); i++) {
                var time = times.getFloat(i);
                var result = interpolator.build(time);
                var vector = result.vector();
                rotations[i][bone] = vector.eval().add(tree.boneRotation);
            }
        }
        return new EulerFrames(times, rotations);
    }

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
            var outward = 0D;
            var inward = Math.acos(Math.min(1F, dot));
            for (int j = from + 1; j <= to; j++) {
                var q = samples.get(j).world[i];
                var fromStart = quaternionDot(a, q);
                var toEnd = quaternionDot(b, q);
                if (!(fromStart > MIN_ROTATION_DOT && toEnd > MIN_ROTATION_DOT)) return false;
                // A single SLERP can only move away from its start and toward its end.
                var away = Math.acos(Math.min(1F, fromStart));
                var toward = Math.acos(Math.min(1F, toEnd));
                if (away + REVERSAL_TOLERANCE < outward || toward > inward + REVERSAL_TOLERANCE) return false;
                outward = Math.max(outward, away);
                inward = Math.min(inward, toward);
            }
        }
        return true;
    }

    private static float quaternionDot(@NotNull Quaternionf a, @NotNull Quaternionf b) {
        return a.x * b.x + a.y * b.y + a.z * b.z + a.w * b.w;
    }

    private static void addRotationSplits(@NotNull List<RotationSample> samples, @NotNull FloatSortedSet floats) {
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

                var cost = unsafeTime[j] + (canInterpolateRotation(samples, i, j) ? 0D : (double) end - start);

                if (!isBetterSplit(cost, count, unsafeTime[i], segments[i])) continue;

                unsafeTime[i] = cost;
                segments[i] = count;
                next[i] = j;

                if (cost == 0 && j == size - 1) break;
            }
        }

        addSplitTimes(samples, next, floats);
    }

    private static boolean isBetterSplit(double cost, int count, double currentCost, int currentCount) {
        return cost < currentCost || cost == currentCost && count < currentCount;
    }

    private static void addSplitTimes(@NotNull List<RotationSample> samples, int[] next, @NotNull FloatSortedSet floats) {
        var last = samples.size() - 1;
        for (int i = next[0]; i > 0 && i < last; i = next[i]) {
            floats.add(samples.get(i).time);
        }
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
        private final @Nullable AnimationTree parent;
        private final @NotNull AnimationTree[] children;
        private final @Nullable BlueprintAnimator.AnimatorData data;
        private final @NotNull Vector3f boneRotation;

        AnimationTree(
            @Nullable AnimationTree parent,
            @NotNull BlueprintElement.Group group,
            @NotNull Function<BlueprintElement.Group, BlueprintAnimator.AnimatorData> function
        ) {
            this.parent = parent;
            this.data = function.apply(group);
            this.boneRotation = group.rotation().toVector();
            children = filterIsInstance(group.children(), BlueprintElement.Group.class)
                .map(g -> new AnimationTree(this, g, function))
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
