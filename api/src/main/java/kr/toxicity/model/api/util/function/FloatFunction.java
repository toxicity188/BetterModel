package kr.toxicity.model.api.util.function;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;

@FunctionalInterface
public interface FloatFunction<T> {
    @NotNull T applyAsFloat(float value);

    static <T> @NotNull FloatConstantFunction<T> of(@NotNull T t) {
        return new FloatConstantFunction<>(Objects.requireNonNull(t));
    }

    default <R> @NotNull FloatFunction<R> map(@NotNull Function<T, R> mapper) {
        if (this instanceof FloatConstantFunction<T>(T t)) {
            return new FloatConstantFunction<>(mapper.apply(t));
        } else {
            return f -> mapper.apply(applyAsFloat(f));
        }
    }
}
