package kr.toxicity.model.api.animation;

import kr.toxicity.model.api.util.function.FloatFunction;
import kr.toxicity.model.api.util.interpolation.VectorInterpolation;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public record VectorPoint(@NotNull FloatFunction<Vector3f> vector, float time, @NotNull VectorInterpolation interpolation) implements Timed {

    public static final VectorPoint EMPTY = new VectorPoint(
            FloatFunction.of(new Vector3f()),
            0F,
            VectorInterpolation.defaultInterpolation()
    );

    public @NotNull Vector3f vector(float f) {
        return vector.applyAsFloat(f);
    }

    public @NotNull VectorPoint time(float newTime) {
        return new VectorPoint(vector, newTime, interpolation);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VectorPoint that)) return false;
        return Float.compare(time, that.time) == 0;
    }

    @Override
    public int hashCode() {
        return Float.hashCode(time);
    }
}
