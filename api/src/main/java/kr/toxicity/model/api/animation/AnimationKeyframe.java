/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.animation;

import kr.toxicity.model.api.bone.BoneMovement;
import kr.toxicity.model.api.util.MathUtil;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Arrays;

import static kr.toxicity.model.api.util.MathUtil.isNotZero;

/**
 * Represents a collection of animation keyframes, optimized for efficient storage and access.
 * <p>
 * This record stores an array of {@link AnimationProgress} objects, which define the state of a bone
 * at specific time intervals. It implements {@link TimedStorage} for indexed access.
 * </p>
 *
 * @param progresses the array of animation progresses
 * @since 2.0.0
 */
public record AnimationKeyframe(
    @NotNull AnimationProgress[] progresses
) implements TimedStorage<AnimationProgress> {

    /**
     * Creates a new builder for constructing an AnimationKeyframe.
     *
     * @param size the number of keyframes
     * @param rotateGlobal whether rotation should be applied globally
     * @return a new builder instance
     * @since 2.0.0
     */
    public static @NotNull Builder builder(int size, boolean rotateGlobal) {
        return new Builder(size, rotateGlobal);
    }

    private record AnimationArray(
        boolean rotateGlobal,
        boolean[] skipInterpolation,
        float[] times,
        float[] position,
        float[] scale,
        float[] rotation
    ) {
        AnimationArray(int size, boolean rotateGlobal) {
            this(
                rotateGlobal,
                new boolean[size],
                new float[size],
                new float[size * 3],
                new float[size * 3],
                new float[size * 3]
            );
        }
    }

    /**
     * Builder for {@link AnimationKeyframe}.
     * <p>
     * This builder allows for efficient population of keyframe data using primitive arrays.
     * </p>
     *
     * @since 2.0.0
     */
    public static final class Builder {
        private final AnimationArray set;
        private final AnimationProgress[] progresses;

        private int index = 0;

        private Builder(int size, boolean rotateGlobal) {
            set = new AnimationArray(size, rotateGlobal);
            progresses = new AnimationProgress[size];
        }

        /**
         * Writes a keyframe data point.
         *
         * @param time the time of the keyframe
         * @param position the position vector
         * @param scale the scale vector
         * @param rotation the rotation vector
         * @param skipInterpolation whether to skip interpolation for this keyframe
         * @since 2.0.0
         */
        public void write(
            float time,
            @NotNull Vector3f position,
            @NotNull Vector3f scale,
            @NotNull Vector3f rotation,
            boolean skipInterpolation
        ) {
            var i = index++;

            var x = i * 3;
            var y = x + 1;
            var z = x + 2;

            set.times[i] = time;
            set.position[x] = position.x;
            set.position[y] = position.y;
            set.position[z] = position.z;
            set.scale[x] = scale.x + 1;
            set.scale[y] = scale.y + 1;
            set.scale[z] = scale.z + 1;
            set.rotation[x] = rotation.x;
            set.rotation[y] = rotation.y;
            set.rotation[z] = rotation.z;
            set.skipInterpolation[i] = skipInterpolation;

            this.progresses[i] = isNotZero(position) || isNotZero(scale) || isNotZero(rotation) ? new ArrayProgress(set, i) : AnimationProgress.empty(time);
        }

        /**
         * Builds the {@link AnimationKeyframe}.
         *
         * @return the created keyframe collection
         * @since 2.0.0
         */
        public @NotNull AnimationKeyframe build() {
            return new AnimationKeyframe(progresses);
        }
    }

    private record ArrayProgress(@NotNull AnimationArray array, int index) implements AnimationProgress {

        @Override
        public @NotNull BoneMovement animate(@NotNull BoneMovement movement, @NotNull BoneMovement dest) {

            var destPos = movement.position().get(dest.position());
            var destScl = movement.scale().get(dest.scale());
            var destRot = movement.rotation().get(dest.rotation());
            var destRawRot = movement.rawRotation().get(dest.rawRotation());

            var position = array.position;
            var scale = array.scale;
            var rotation = array.rotation;

            var x = index * 3;
            var y = x + 1;
            var z = x + 2;

            destPos.add(position[x], position[y], position[z]);
            destScl.mul(scale[x], scale[y], scale[z]);
            MathUtil.toQuaternion(destRawRot.add(rotation[x], rotation[y], rotation[z]), destRot);

            return dest;
        }

        @Override
        public boolean skipInterpolation() {
            return array.skipInterpolation[index];
        }

        @Override
        public boolean globalRotation() {
            return array.rotateGlobal;
        }

        @Override
        public float time() {
            return array.times[index];
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

    /**
     * Converts this keyframe collection to a storage of empty progresses.
     *
     * @return a new timed storage with empty progresses
     * @since 2.0.0
     */
    public @NotNull TimedStorage<AnimationProgress> toEmpty() {
        return TimedStorage.listOf(Arrays.stream(progresses)
            .map(AnimationProgress::toEmpty)
            .toList());
    }
}
