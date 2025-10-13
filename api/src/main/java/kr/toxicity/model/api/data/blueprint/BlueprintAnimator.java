/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
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
 * A movement of each group.
 * @param name group name
 * @param keyframe keyframes
 */
public record BlueprintAnimator(
        @NotNull String name,
        @NotNull @Unmodifiable List<AnimationMovement> keyframe
) {


    /**
     * Animation data
     * @param name name
     * @param position position
     * @param scale scale
     * @param rotation rotation
     */
    public record AnimatorData(
            @NotNull String name,
            @NotNull List<VectorPoint> position,
            @NotNull List<VectorPoint> scale,
            @NotNull List<VectorPoint> rotation
    ) {
        /**
         * Gets flatten points
         * @return all points
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
     * Gets loop iterator.
     * @param type type
     * @return iterator
     */
    public @NotNull AnimationIterator<AnimationMovement> iterator(@NotNull AnimationIterator.Type type) {
        return type.create(keyframe);
    }
}
