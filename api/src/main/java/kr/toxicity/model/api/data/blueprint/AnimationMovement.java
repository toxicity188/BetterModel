package kr.toxicity.model.api.data.blueprint;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public record AnimationMovement(
        long time,
        @Nullable Vector3f transform,
        @Nullable Vector3f scale,
        @Nullable Vector3f rotation
) implements Comparable<AnimationMovement> {

    @Override
    public int compareTo(@NotNull AnimationMovement o) {
        return Long.compare(time, o.time);
    }
}
