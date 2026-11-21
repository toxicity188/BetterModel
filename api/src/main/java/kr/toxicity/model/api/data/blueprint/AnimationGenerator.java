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
import kr.toxicity.model.api.animation.AnimationKeyframe;
import kr.toxicity.model.api.animation.keyframe.KeyframePoint;
import kr.toxicity.model.api.bone.BoneName;
import kr.toxicity.model.api.util.InterpolationUtil;
import kr.toxicity.model.api.util.MathUtil;
import kr.toxicity.model.api.util.interpolator.VectorInterpolator;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Function;
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

    private static final float MIN_ROTATION_DOT = (float) Math.cos(Math.toRadians(179.9 / 2));
    private static final double REVERSAL_TOLERANCE = Math.toRadians(2.0 / 2);
    private static final int MAX_ROTATION_SAMPLES = 128;

    private final @NotNull AnimationTree[] trees;
    private final int[] parents;

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
            .flatMap(BlueprintAnimator.AnimatorData::allPoints), KeyframePoint::time, () -> new FloatAVLTreeSet(MathUtil.FRAME_COMPARATOR));
        floatSet.add(0F);
        floatSet.add(length);
        var generator = new AnimationGenerator(g -> pointMap.get(g.name()), children);
        InterpolationUtil.insertLerpFrame(floatSet);
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
     * <pre>{@code
     * generator.interpolateRotation(frameTimes);
     * }</pre>
     *
     * @param floats the set of keyframe times to update
     * @since 1.15.2
     */
    public void interpolateRotation(@NotNull FloatSortedSet floats) {
        if (Arrays.stream(trees).noneMatch(t -> t.data != null && !t.data.rotation().isEmpty())) return;
        var frames = sampleEuler(new FloatArrayList(floats), true);
        var candidates = rotationCandidates(frames);
        if (candidates.times().isEmpty()) return;

        var extraFrames = sampleEuler(candidates.times(), false);
        for (int i = 1; i < frames.size(); i++) {
            if (candidates.from(i) == candidates.to(i)) continue;
            var samples = rotationSamples(i, frames, candidates, extraFrames);
            if (canInterpolateRotation(samples, 0, samples.size() - 1)) continue;
            addRotationSplits(samples, floats);
        }
    }

    private record EulerFrames(
        @NotNull FloatArrayList times,
        @NotNull Vector3f[][] rotations,
        boolean[] nonlinear
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

        boolean isNonlinear(int index) {
            return nonlinear[index];
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

    private @NotNull RotationCandidates rotationCandidates(@NotNull EulerFrames frames) {
        var candidates = new FloatArrayList();
        var offsets = new int[frames.size()];
        for (int i = 1; i < frames.size(); i++) {
            var start = frames.time(i - 1);
            var end = frames.time(i);
            var earliest = start + MathUtil.MINECRAFT_TICK_SECONDS;
            var latest = end - MathUtil.MINECRAFT_TICK_SECONDS;
            if (frameSpacing(start, earliest) && frameSpacing(earliest, end)) {
                var travel = rotationTravel(frames.rotation(i - 1), frames.rotation(i));
                if (frames.isNonlinear(i) || travel > 2F) {
                    // Keep Euler turns in the sampling budget before converting poses to quaternions.
                    var segments = Math.clamp((int) Math.ceil(travel / 45F), frames.isNonlinear(i) ? 16 : 2, MAX_ROTATION_SAMPLES);
                    var interval = new FloatAVLTreeSet();
                    interval.add(earliest);
                    if (latest > earliest) interval.add(latest);
                    for (int j = 1; j < segments; j++) {
                        var time = InterpolationUtil.lerp(start, end, (float) j / segments);
                        if (time > start && time < end) interval.add(time);
                    }
                    candidates.addAll(interval);
                }
            }
            offsets[i] = candidates.size();
        }
        return new RotationCandidates(candidates, offsets);
    }

    private float rotationTravel(@NotNull Vector3f[] start, @NotNull Vector3f[] end) {
        var travel = new float[trees.length];
        var max = 0F;
        for (int i = 0; i < trees.length; i++) {
            var a = start[i];
            var b = end[i];
            travel[i] = Math.abs(b.x - a.x) + Math.abs(b.y - a.y) + Math.abs(b.z - a.z);
            if (inheritsRotation(i)) travel[i] += travel[parents[i]];
            max = Math.max(max, travel[i]);
        }
        return max;
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

    private @NotNull EulerFrames sampleEuler(@NotNull FloatArrayList times, boolean inspectNonlinear) {
        var rotations = new Vector3f[times.size()][trees.length];
        var nonlinear = new boolean[times.size()];
        for (int bone = 0; bone < trees.length; bone++) {
            var tree = trees[bone];
            var points = tree.data == null ? List.<KeyframePoint>of() : tree.data.rotation();
            if (points.isEmpty()) {
                for (var rotation : rotations) rotation[bone] = tree.boneRotation;
                continue;
            }
            var interpolator = InterpolationUtil.interpolatorFor(points);
            var point = 0;
            for (int i = 0; i < times.size(); i++) {
                var time = times.getFloat(i);
                var result = interpolator.build(time);
                var vector = result.vector();
                rotations[i][bone] = vector.eval().add(tree.boneRotation);
                if (!inspectNonlinear) continue;
                if (!vector.isConstant()) {
                    nonlinear[i] = true;
                    if (i + 1 < times.size()) nonlinear[i + 1] = true;
                }
                if (i == 0 || points.size() < 2) continue;
                var start = times.getFloat(i - 1);
                while (point + 1 < points.size() && points.get(point + 1).time() <= start) point++;
                if (point + 1 < points.size() && time > points.get(point).time()) {
                    nonlinear[i] |= points.get(point).interpolator() != VectorInterpolator.LINEAR;
                }
            }
        }
        return new EulerFrames(times, rotations, nonlinear);
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
        var last = samples.size() - 1;
        var next = new int[samples.size()];
        var segments = new int[samples.size()];
        var unsafeTime = new double[samples.size()];
        for (int i = last - 1; i >= 0; i--) {
            var start = samples.get(i).time;
            if (i != 0 && !frameSpacing(start, samples.get(last).time)) continue;
            unsafeTime[i] = Double.POSITIVE_INFINITY;
            for (int j = last; j > i; j--) {
                var end = samples.get(j).time;
                if (!frameSpacing(start, end)) break;
                if (j != last && !frameSpacing(end, samples.get(last).time)) continue;
                var count = segments[j] + 1;
                if (unsafeTime[j] > unsafeTime[i] || unsafeTime[j] == unsafeTime[i] && count >= segments[i]) continue;
                // If tick spacing prevents a safe path, minimize the duration of unsafe segments.
                var cost = unsafeTime[j] + (canInterpolateRotation(samples, i, j) ? 0D : (double) end - start);
                if (cost > unsafeTime[i] || cost == unsafeTime[i] && count >= segments[i]) continue;
                unsafeTime[i] = cost;
                segments[i] = count;
                next[i] = j;
                if (cost == 0 && j == last) break;
            }
        }
        for (int i = next[0]; i > 0 && i < last; i = next[i]) floats.add(samples.get(i).time);
    }

    /**
     * Inserts keyframes for step interpolation (non-continuous transitions).
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
            var time = points.get(i).time() - MathUtil.MINECRAFT_TICK_SECONDS;
            if (time < 0 || time - before.time() < 0) continue;
            floats.add(time);
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
