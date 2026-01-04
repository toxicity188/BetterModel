/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.blueprint;

import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.animation.AnimationMovement;
import kr.toxicity.model.api.animation.VectorPoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.stream.Stream;

/**
 * Represents the processed animation data for a single bone within a model blueprint.
 * <p>
 * This record holds the sequence of keyframes that define the bone's movement over time.
 * </p>
 *
 * @param name the name of the bone this animator applies to
 * @param keyframe a list of animation movements representing the keyframes
 * @since 1.15.2
 */
public record BlueprintAnimator(
    @NotNull String name,
    @NotNull @Unmodifiable List<AnimationMovement> keyframe
) {

    /**
     * Holds the raw, separated animation data points for a bone before final processing.
     *
     * @param name the name of the bone
     * @param position a list of position keyframes
     * @param scale a list of scale keyframes
     * @param rotation a list of rotation keyframes
     * @param rotationGlobal whether the rotation is applied globally
     * @since 1.15.2
     */
    public record AnimatorData(
        @NotNull String name,
        @NotNull List<VectorPoint> position,
        @NotNull List<VectorPoint> scale,
        @NotNull List<VectorPoint> rotation,
        boolean rotationGlobal
    ) {
        /**
         * Returns a stream containing all keyframe points (position, scale, and rotation).
         *
         * @return a stream of all vector points
         * @since 1.15.2
         */
        public @NotNull Stream<VectorPoint> allPoints() {
            return Stream.concat(
                Stream.concat(
                    position.stream(),
                    scale.stream()
                ),
                rotation.stream()
            );
        }
    }

    /**
     * Creates an iterator for the keyframes based on a specified loop type.
     *
     * @param type the loop type (e.g., play_once, loop)
     * @return an animation iterator
     * @since 1.15.2
     */
    public @NotNull AnimationIterator<AnimationMovement> iterator(@NotNull AnimationIterator.Type type) {
        return type.create(keyframe);
    }
}
