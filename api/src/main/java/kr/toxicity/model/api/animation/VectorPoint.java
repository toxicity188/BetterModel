package kr.toxicity.model.api.animation;

import kr.toxicity.model.api.util.interpolation.VectorInterpolation;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public record VectorPoint(@NotNull Vector3f vector, float time, @NotNull VectorInterpolation interpolation) implements Comparable<VectorPoint> {

    public static final VectorPoint EMPTY = new VectorPoint(
            new Vector3f(),
            0F,
            VectorInterpolation.defaultInterpolation()
    );

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VectorPoint that)) return false;
        return Float.compare(time, that.time) == 0;
    }

    @Override
    public int compareTo(@NotNull VectorPoint o) {
        return Float.compare(time, o.time);
    }

    @Override
    public int hashCode() {
        return Float.hashCode(time);
    }
}
