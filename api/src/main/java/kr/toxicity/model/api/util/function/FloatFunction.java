package kr.toxicity.model.api.util.function;

import it.unimi.dsi.fastutil.floats.Float2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;

@FunctionalInterface
public interface FloatFunction<T> {
    @NotNull T apply(float value);

    static <T> @NotNull FloatConstantFunction<T> of(@NotNull T t) {
        return new FloatConstantFunction<>(Objects.requireNonNull(t));
    }

    default <R> @NotNull FloatFunction<R> map(@NotNull Function<T, R> mapper) {
        if (this instanceof FloatConstantFunction<T>(T value)) {
            return of(mapper.apply(value));
        } else {
            return f -> mapper.apply(apply(f));
        }
    }

    default @NotNull FloatFunction<T> memoize() {
        if (this instanceof FloatConstantFunction<T>) return this;
        var map = new Float2ObjectOpenHashMap<T>();
        return f -> map.computeIfAbsent(f, this::apply);
    }
}
