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

import static kr.toxicity.model.api.util.InterpolationUtil.interpolatorFor;
import static kr.toxicity.model.api.util.InterpolationUtil.roundTime;

public record AnimationKeyframe(
    @NotNull AnimationProgress[] progresses
) implements TimedStorage<AnimationProgress> {

    /**
     * Builds animation keyframe
     * @param position position point
     * @param rotation rotation point
     * @param scale scale point
     * @param rotationGlobal rotation global
     * @param points keyframe time set
     * @return animation keyframe
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
            rotationGlobal,
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
        return keyframe.build();
    }

    private static final class Builder {
        private final int size;
        private final boolean rotateGlobal;
        private final boolean[] skipInterpolation;
        private final float[] times;

        private final KeyframeTimeline.Builder position, rotation, scale;

        private int index = 0;

        private Builder(int size, boolean rotateGlobal, boolean positionConstant, boolean rotationConstant, boolean scaleConstant) {
            this.size = size;
            this.rotateGlobal = rotateGlobal;
            times = new float[size];
            skipInterpolation = new boolean[size];
            position = positionConstant ? KeyframeTimeline.constantBuilder(size) : KeyframeTimeline.runtimeBuilder(size);
            rotation = rotationConstant ? KeyframeTimeline.constantBuilder(size) : KeyframeTimeline.runtimeBuilder(size);
            scale = scaleConstant ? KeyframeTimeline.constantBuilder(size) : KeyframeTimeline.runtimeBuilder(size);
        }

        public void write(
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

        public @NotNull AnimationKeyframe build() {
            var progresses = new AnimationProgress[size];
            var posBuild = position.build();
            var rotBuild = rotation.build();
            var scaleBuild = scale.build();
            for (int i = 0; i < size; i++) {
            progresses[i] = posBuild.isZero(i) && rotBuild.isZero(i) && scaleBuild.isZero(i)
                ? AnimationProgress.empty(times[i], this.skipInterpolation[i], rotateGlobal)
                : new ArrayProgress(posBuild, rotBuild, scaleBuild, i, times[i], this.skipInterpolation[i], rotateGlobal);
            }
            return new AnimationKeyframe(progresses);
        }
    }

    private record ArrayProgress(
        @NotNull KeyframeTimeline posTimeline,
        @NotNull KeyframeTimeline rotTimeline,
        @NotNull KeyframeTimeline scaleTimeline,
        int index,
        float time,
        boolean skipInterpolation,
        boolean rotateGlobal
    ) implements AnimationProgress {

        @Override
        public @NotNull BoneMovement animate(@NotNull KeyframeRuntimeContext context, @NotNull BoneMovement movement, @NotNull BoneMovement dest) {

            var destPos = movement.position().get(dest.position());
            var destScl = movement.scale().get(dest.scale());
            var destRot = movement.rotation().get(dest.rotation());
            var destRawRot = movement.rawRotation().get(dest.rawRotation());

            posTimeline.apply(index, context, destPos, KeyframeOperator.ADD);
            scaleTimeline.apply(index, context, destScl, KeyframeOperator.SCALE_MUL);
            MathUtil.toQuaternion(rotTimeline.apply(index, context, destRawRot, KeyframeOperator.ADD), destRot);

            return dest;
        }

        @Override
        public boolean skipInterpolation() {
            return skipInterpolation;
        }

        @Override
        public boolean globalRotation() {
            return rotateGlobal;
        }
    }

    @Override
    public @NotNull AnimationProgress get(int i) {
        return progresses[i];
    }

    @Override
    public @NotNull AnimationProgress getLast() {
        return get(progresses.length - 1);
    }

    @Override
    public int size() {
        return progresses.length;
    }

    public @NotNull TimedStorage<AnimationProgress> toEmpty() {
        return TimedStorage.listOf(Arrays.stream(progresses)
            .map(AnimationProgress::toEmpty)
            .toList());
    }
}
