package kr.toxicity.model.api.entity;

import kr.toxicity.model.api.util.MathUtil;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public record TrackerMovement(
        @NotNull Vector3f transform,
        @NotNull Vector3f scale,
        @NotNull Vector3f rotation
) {
    public @NotNull EntityMovement plus(@NotNull EntityMovement movement) {
        var q = MathUtil.toQuaternion(rotation);
        return new EntityMovement(
                new Vector3f(transform).add(new Vector3f(movement.transform()).rotate(q).mul(scale)),
                new Vector3f(scale).mul(movement.scale()),
                new Quaternionf(q).mul(movement.rotation()),
                new Vector3f(movement.rawRotation())
        );
    }
    public @NotNull TrackerMovement copy() {
        return new TrackerMovement(
                new Vector3f(transform),
                new Vector3f(scale),
                new Vector3f(rotation)
        );
    }
}
