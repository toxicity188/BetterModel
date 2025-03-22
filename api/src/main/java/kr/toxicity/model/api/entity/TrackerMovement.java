package kr.toxicity.model.api.entity;

import kr.toxicity.model.api.util.MathUtil;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A tracker's movement
 * @param transform position
 * @param scale scale
 * @param rotation rotation
 */
public record TrackerMovement(
        @NotNull Vector3f transform,
        @NotNull Vector3f scale,
        @NotNull Vector3f rotation
) {

    /**
     * Adds each group's movement.
     * @param movement movement
     * @return movement
     */
    public @NotNull EntityMovement plus(@NotNull EntityMovement movement) {
        var q = MathUtil.toQuaternion(rotation);
        return new EntityMovement(
                new Vector3f(transform).add(new Vector3f(movement.transform()).rotate(q).mul(scale)),
                new Vector3f(scale).mul(movement.scale()),
                new Quaternionf(q).mul(movement.rotation()),
                new Vector3f(movement.rawRotation())
        );
    }
}
