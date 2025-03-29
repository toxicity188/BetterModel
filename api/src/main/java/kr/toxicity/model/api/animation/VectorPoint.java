package kr.toxicity.model.api.animation;

import kr.toxicity.model.api.util.VectorInterpolation;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Objects;

public record VectorPoint(@NotNull Vector3f vector, float time, @NotNull VectorInterpolation interpolation) {

    public static final VectorPoint EMPTY = new VectorPoint(
            new Vector3f(),
            0F,
            VectorInterpolation.LINEAR
    );

    public static @NotNull VectorPoint emptyOf(float time) {
        return new VectorPoint(new Vector3f(), time, VectorInterpolation.LINEAR);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VectorPoint that)) return false;
        return Float.compare(time, that.time) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(time);
    }
}
