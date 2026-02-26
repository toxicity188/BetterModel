/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.bone;

import kr.toxicity.model.api.util.InterpolationUtil;
import kr.toxicity.model.api.util.MathUtil;
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
            new Vector3f(1),
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

    /**
     * Linearly interpolates between this movement and another movement.
     *
     * @param to the target movement
     * @param alpha the interpolation factor (0.0 to 1.0)
     * @param dest the destination movement to store the result
     * @return the destination movement
     * @since 2.1.0
     */
    public @NotNull BoneMovement lerp(@NotNull BoneMovement to, float alpha, @NotNull BoneMovement dest) {
        InterpolationUtil.lerp(position, to.position, alpha, dest.position);
        InterpolationUtil.lerp(scale, to.scale, alpha, dest.scale);
        MathUtil.toQuaternion(InterpolationUtil.lerp(rawRotation, to.rawRotation, alpha, dest.rawRotation), dest.rotation);
        return dest;
    }
}
