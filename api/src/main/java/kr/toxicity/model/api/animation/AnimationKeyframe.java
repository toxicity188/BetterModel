/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.animation;

import it.unimi.dsi.fastutil.floats.FloatSortedSet;
import kr.toxicity.model.api.animation.keyframe.*;
import kr.toxicity.model.api.bone.BoneMovement;
import kr.toxicity.model.api.util.MathUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static kr.toxicity.model.api.util.InterpolationUtil.interpolatorFor;
import static kr.toxicity.model.api.util.InterpolationUtil.roundTime;

/**
 * Stores the ordered animation progresses for a keyframe timeline.
 *
 * <pre>{@code
 * AnimationKeyframe keyframe = new AnimationKeyframe(new AnimationProgress[0]);
 * int count = keyframe.size();
 * }</pre>
 *
 * @param progresses the ordered animation progresses
 * @since 3.7.0
 */
public record AnimationKeyframe(
    @NotNull AnimationProgress[] progresses
) implements TimedStorage<AnimationProgress> {

    /**
     * Builds a timeline by sampling the transform channels at the supplied points.
     *
     * @param position position keyframe points
     * @param rotation rotation keyframe points
     * @param scale scale keyframe points
     * @param rotationGlobal whether rotation is global
     * @param points sorted sample times
     * @return the sampled animation keyframe
     * @since 3.7.0
     */
    @NotNull
    public static AnimationKeyframe build(
        @NotNull List<KeyframePoint> position,
        @NotNull List<KeyframePoint> rotation,
        @NotNull List<KeyframePoint> scale,
        boolean rotationGlobal,
        @NotNull FloatSortedSet points
    ) {
        var pp = interpolatorFor(position);
        var rp = interpolatorFor(rotation);
        var sp = interpolatorFor(scale);
        var keyframe = new Builder(
            points.size(),
            position.stream().allMatch(KeyframePoint::isConstant),
            rotation.stream().allMatch(KeyframePoint::isConstant),
            scale.stream().allMatch(KeyframePoint::isConstant)
        );
        var before = 0F;
        var iterator = points.iterator();
        while (iterator.hasNext()) {
            var f = iterator.nextFloat();
            var pr = pp.build(f);
            var rr = rp.build(f);
            var sr = sp.build(f);
            keyframe.write(
                pr.vector(),
                rr.vector(),
                sr.vector(),
                roundTime(f - before),
                pr.skipInterpolation() || sr.skipInterpolation() || rr.skipInterpolation()
            );
            before = f;
        }
        return keyframe.build(rotationGlobal);
    }

    private static final class Builder {
        final int size;
        final boolean[] skipInterpolation;
        final float[] times;

        final KeyframeTimeline.Builder position, rotation, scale;

        int index = 0;

        Builder(int size, boolean positionConstant, boolean rotationConstant, boolean scaleConstant) {
            this.size = size;
            times = new float[size];
            skipInterpolation = new boolean[size];
            position = positionConstant ? KeyframeTimeline.constantBuilder(size) : KeyframeTimeline.runtimeBuilder(size);
            rotation = rotationConstant ? KeyframeTimeline.constantBuilder(size) : KeyframeTimeline.runtimeBuilder(size);
            scale = scaleConstant ? KeyframeTimeline.constantBuilder(size) : KeyframeTimeline.runtimeBuilder(size);
        }

        void write(
            @NotNull KeyframeVector pos,
            @NotNull KeyframeVector rot,
            @NotNull KeyframeVector scl,
            float time,
            boolean skipInterpolation
        ) {
            position.append(pos);
            rotation.append(rot);
            scale.append(scl);

            var i = index++;

            this.times[i] = time;
            this.skipInterpolation[i] = skipInterpolation;
        }

        @NotNull AnimationKeyframe build(boolean rotateGlobal) {
            var data = new ProgressData(
                position.build(),
                rotation.build(),
                scale.build(),
                rotateGlobal
            );
            return new AnimationKeyframe(IntStream.range(0, size)
                .mapToObj(i -> data.isZero(i) ? AnimationProgress.empty(times[i], this.skipInterpolation[i], rotateGlobal) : new ArrayProgress(data, i, times[i], this.skipInterpolation[i]))
                .toArray(AnimationProgress[]::new));
        }
    }

    private record ProgressData(
        @NotNull KeyframeTimeline position,
        @NotNull KeyframeTimeline rotation,
        @NotNull KeyframeTimeline scale,
        boolean rotateGlobal
    ) {
        boolean isZero(int index) {
            return position.isZero(index) && rotation.isZero(index) && scale.isZero(index);
        }
    }

    private record ArrayProgress(
        @NotNull ProgressData data,
        int index,
        float time,
        boolean skipInterpolation
    ) implements AnimationProgress {

        @Override
        public @NotNull BoneMovement animate(@NotNull KeyframeRuntimeContext context, @NotNull BoneMovement movement, @NotNull BoneMovement dest) {

            var destPos = movement.position().get(dest.position());
            var destScl = movement.scale().get(dest.scale());
            var destRot = movement.rotation().get(dest.rotation());
            var destRawRot = movement.rawRotation().get(dest.rawRotation());

            data.position.apply(index, context, destPos, KeyframeOperator.ADD);
            data.scale.apply(index, context, destScl, KeyframeOperator.SCALE_MUL);
            MathUtil.toQuaternion(data.rotation.apply(index, context, destRawRot, KeyframeOperator.ADD), destRot);

            return dest;
        }

        @Override
        public boolean skipInterpolation() {
            return skipInterpolation;
        }

        @Override
        public boolean globalRotation() {
            return data.rotateGlobal;
        }
    }

    /**
     * Returns the progress at the specified index.
     *
     * @param i the progress index
     * @return the progress at the index
     * @throws IndexOutOfBoundsException if the index is outside the timeline
     */
    @Override
    public @NotNull AnimationProgress get(int i) {
        return progresses[i];
    }

    /**
     * Returns the last progress.
     *
     * @return the last progress
     * @throws IndexOutOfBoundsException if the timeline is empty
     */
    @Override
    public @NotNull AnimationProgress getLast() {
        return get(progresses.length - 1);
    }

    /**
     * Returns the number of progresses.
     *
     * @return the number of progresses
     */
    @Override
    public int size() {
        return progresses.length;
    }

    /**
     * Returns a timeline containing an empty equivalent of each progress.
     *
     * @return the empty progress timeline
     * @since 3.7.0
     */
    public @NotNull TimedStorage<AnimationProgress> toEmpty() {
        return TimedStorage.listOf(Arrays.stream(progresses)
            .map(AnimationProgress::toEmpty)
            .toList());
    }
}
