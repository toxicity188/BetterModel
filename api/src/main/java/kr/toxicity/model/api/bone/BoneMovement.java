/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.bone;

import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Represents the transformation state of a single bone, including its position, scale, and rotation.
 * <p>
 * This record is used to calculate the final transformation of a bone after applying animations.
 * </p>
 *
 * @param position the local position of the bone
 * @param scale the local scale of the bone
 * @param rotation the final local rotation of the bone as a quaternion
 * @param rawRotation the local rotation of the bone in Euler angles (degrees) before being converted to a quaternion
 * @since 1.15.2
 */
public record BoneMovement(
    @NotNull Vector3f position,
    @NotNull Vector3f scale,
    @NotNull Quaternionf rotation,
    @NotNull Vector3f rawRotation
) {

    /**
     * Creates a new BoneMovement with default (identity) transformations.
     * @since 1.15.2
     */
    public BoneMovement() {
        this(
            new Vector3f(),
            new Vector3f(),
            new Quaternionf(),
            new Vector3f()
        );
    }

    /**
     * Copies the values from another BoneMovement into this one.
     *
     * @param movement the source movement
     * @return this movement instance
     * @since 1.15.2
     */
    public @NotNull BoneMovement set(@NotNull BoneMovement movement) {
        position.set(movement.position);
        scale.set(movement.scale);
        rotation.set(movement.rotation);
        rawRotation.set(movement.rawRotation);
        return this;
    }
}
