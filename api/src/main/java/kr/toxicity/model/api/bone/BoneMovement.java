package kr.toxicity.model.api.bone;

import kr.toxicity.model.api.animation.AnimationMovement;
import kr.toxicity.model.api.util.MathUtil;
import kr.toxicity.model.api.util.InterpolationUtil;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A bone movement.
 * @param transform position
 * @param scale scale
 * @param rotation rotation
 * @param rawRotation raw rotation
 */
public record BoneMovement(
        @NotNull Vector3f transform,
        @NotNull Vector3f scale,
        @NotNull Quaternionf rotation,
        @NotNull Vector3f rawRotation
) {

    public static final BoneMovement EMPTY = new BoneMovement(
            new Vector3f(),
            new Vector3f(1),
            new Quaternionf(),
            new Vector3f()
    );

    /**
     * Animates this movement.
     * @param movement animation
     * @return animated movement
     */
    public @NotNull BoneMovement plus(@NotNull AnimationMovement movement) {
        var mov = movement.transform();
        var scl = movement.scale();
        var rot = movement.rotation();
        var rawRot = rot == null ? new Vector3f(rawRotation) : new Vector3f(rot).add(rawRotation);
        return new BoneMovement(
                mov == null ? new Vector3f(transform) : new Vector3f(transform).add(mov),
                scl == null ? new Vector3f(scale) : new Vector3f(scale).mul(new Vector3f(scl).add(1, 1, 1)),
                rot == null ? new Quaternionf(rotation) : MathUtil.toQuaternion(rawRot),
                rawRot
        );
    }

    public boolean isVisible() {
        return InterpolationUtil.isVisible(scale);
    }
}
