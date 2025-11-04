/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.bone;

import kr.toxicity.model.api.animation.AnimationMovement;
import kr.toxicity.model.api.util.MathUtil;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A bone movement.
 * @param position position
 * @param scale scale
 * @param rotation rotation
 * @param rawRotation raw rotation
 */
public record BoneMovement(
        @NotNull Vector3f position,
        @NotNull Vector3f scale,
        @NotNull Quaternionf rotation,
        @NotNull Vector3f rawRotation
) {
    /**
     * Animates this movement.
     * @param movement animation
     * @return animated movement
     */
    public @NotNull BoneMovement plus(@NotNull AnimationMovement movement) {
        var mov = movement.position();
        var scl = movement.scale();
        var rot = movement.rotation();
        var rawRot = rot == null ? new Vector3f(rawRotation) : new Vector3f(rot).add(rawRotation);
        return new BoneMovement(
                mov == null ? new Vector3f(position) : new Vector3f(position).add(mov),
                scl == null ? new Vector3f(scale) : new Vector3f(scale).mul(new Vector3f(scl).add(1, 1, 1)),
                rot == null ? new Quaternionf(rotation) : MathUtil.toQuaternion(rawRot),
                rawRot
        );
    }
}
