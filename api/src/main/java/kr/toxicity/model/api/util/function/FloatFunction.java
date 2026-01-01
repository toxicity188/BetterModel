/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.util.function;

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import kr.toxicity.model.api.util.MathUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;

/**
 * Float function
 * @param <T> type
 */
@FunctionalInterface
public interface FloatFunction<T> {

    /**
     * Applies float
     * @param value float value
     * @return applied value
     */
    @NotNull T apply(float value);

    /**
     * Creates constant function by given value
     * @param t value
     * @return constant function
     * @param <T> type
     */
    static <T> @NotNull FloatConstantFunction<T> of(@NotNull T t) {
        return new FloatConstantFunction<>(Objects.requireNonNull(t));
    }

    /**
     * Maps this function to new type
     * @param mapper mapper
     * @return mapped function
     * @param <R> return type
     */
    default <R> @NotNull FloatFunction<R> map(@NotNull Function<T, R> mapper) {
        if (this instanceof FloatConstantFunction<T>(T value)) {
            return of(mapper.apply(value));
        } else {
            return f -> mapper.apply(apply(f));
        }
    }

    /**
     * Memoize this function
     * @return memoized function
     */
    default @NotNull FloatFunction<T> memoize() {
        if (this instanceof FloatConstantFunction<T>) return this;
        var map = new Int2ReferenceOpenHashMap<T>();
        return f -> map.computeIfAbsent(MathUtil.similarHashCode(f), i -> apply(f));
    }
}
