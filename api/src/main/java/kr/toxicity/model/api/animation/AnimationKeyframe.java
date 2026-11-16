/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.animation;

import kr.toxicity.model.api.animation.keyframe.KeyframeOperator;
import kr.toxicity.model.api.animation.keyframe.KeyframeRuntimeContext;
import kr.toxicity.model.api.animation.keyframe.KeyframeTimeline;
import kr.toxicity.model.api.animation.keyframe.KeyframeVector;
import kr.toxicity.model.api.bone.BoneMovement;
import kr.toxicity.model.api.util.MathUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public record AnimationKeyframe(
    @NotNull AnimationProgress[] progresses
) implements TimedStorage<AnimationProgress> {

    public static @NotNull Builder builder(int size, boolean rotateGlobal, boolean positionConstant, boolean rotationConstant, boolean scaleConstant) {
        return new Builder(size, rotateGlobal, positionConstant, rotationConstant, scaleConstant);
    }

    public static final class Builder {
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

        public void writePosition(@NotNull KeyframeVector keyframe) {
            position.append(keyframe);
        }

        public void writeRotation(@NotNull KeyframeVector keyframe) {
            rotation.append(keyframe);
        }

        public void writeScale(@NotNull KeyframeVector keyframe) {
            scale.append(keyframe);
        }

        public void writeExtra(
            float time,
            boolean skipInterpolation
        ) {
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
