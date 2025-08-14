package kr.toxicity.model.api.animation;

import kr.toxicity.model.api.util.function.FloatFunction;
import kr.toxicity.model.api.util.interpolator.VectorInterpolator;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public record VectorPoint(@NotNull FloatFunction<Vector3f> _vector, float time, @NotNull VectorInterpolator interpolator) implements Timed {

    public static final VectorPoint EMPTY = new VectorPoint(
            FloatFunction.of(new Vector3f()),
            0F,
            VectorInterpolator.defaultInterpolator()
    );

    public @NotNull Vector3f vector(float f) {
        return _vector.apply(f);
    }
    public @NotNull Vector3f vector() {
        return vector(time);
    }

    public @NotNull VectorPoint time(float newTime) {
        return new VectorPoint(_vector, newTime, interpolator);
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
