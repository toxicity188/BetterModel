package kr.toxicity.model.api.animation;

import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public record AnimationPoint(
        @NotNull VectorPoint position,
        @NotNull VectorPoint rotation,
        @NotNull VectorPoint scale
) {
    public @NotNull AnimationMovement toMovement() {
        return new AnimationMovement(
                position.time(),
                checkNull(position.vector()) ? null : position.vector(),
                checkNull(scale.vector()) ? null : scale.vector(),
                checkNull(rotation.vector()) ? null : rotation.vector()
        );
    }

    private static boolean checkNull(@NotNull Vector3f vector3f) {
        return vector3f.x() == 0F && vector3f.y() == 0F && vector3f.z() == 0F;
    }
}
