package kr.toxicity.model.api.util.function;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import kr.toxicity.model.api.util.MathUtil;
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
        var map = new Int2ObjectOpenHashMap<T>();
        return f -> map.computeIfAbsent(MathUtil.similarHashCode(f), i -> apply(f));
    }
}
