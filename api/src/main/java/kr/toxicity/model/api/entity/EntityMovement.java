package kr.toxicity.model.api.entity;

import kr.toxicity.model.api.data.blueprint.AnimationMovement;
import kr.toxicity.model.api.util.MathUtil;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public record EntityMovement(
        @NotNull Vector3f transform,
        @NotNull Vector3f scale,
        @NotNull Quaternionf rotation,
        @NotNull Vector3f rawRotation
) {
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
    public @NotNull EntityMovement plus(@NotNull EntityMovement movement) {
        return new EntityMovement(
                new Vector3f(transform).add(movement.transform),
                new Vector3f(scale).mul(movement.scale),
                new Quaternionf(rotation).mul(movement.rotation),
                new Vector3f(rawRotation)
        );
    }
    public @NotNull EntityMovement copy() {
        return new EntityMovement(
                new Vector3f(transform),
                new Vector3f(scale),
                new Quaternionf(rotation),
                new Vector3f(rawRotation)
        );
    }
}
