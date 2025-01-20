package kr.toxicity.model.api.entity;

import kr.toxicity.model.api.data.blueprint.AnimationMovement;
import kr.toxicity.model.api.util.MathUtil;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A entity movement.
 * @param transform position
 * @param scale scale
 * @param rotation rotation
 * @param rawRotation raw rotation
 */
public record EntityMovement(
        @NotNull Vector3f transform,
        @NotNull Vector3f scale,
        @NotNull Quaternionf rotation,
        @NotNull Vector3f rawRotation
) {
    /**
     * Animates this movement.
     * @param movement animation
     * @return animated movement
     */
    public @NotNull EntityMovement plus(@NotNull AnimationMovement movement) {
        var mov = movement.transform();
        var scl = movement.scale();
        var rot = movement.rotation();
        var rawRot = rot == null ? new Vector3f(rawRotation) : new Vector3f(rot).add(rawRotation);
        return new EntityMovement(
                mov == null ? new Vector3f(transform) : new Vector3f(transform).add(mov),
                scl == null ? new Vector3f(scale) : new Vector3f(scale).mul(new Vector3f(scl).add(1, 1, 1)),
                rot == null ? new Quaternionf(rotation) : MathUtil.toQuaternion(MathUtil.blockBenchToDisplay(rawRot)),
                rawRot
        );
    }

    /**
     * Adds other position.
     * @param position position
     * @return new movement
     */
    public @NotNull EntityMovement plus(@NotNull Vector3f position) {
        return new EntityMovement(
                new Vector3f(transform).add(position),
                scale,
                rotation,
                rawRotation
        );
    }
}
