/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.raw;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Holds the context and state during the model loading process.
 * <p>
 * This class provides access to all parts of the raw model data and accumulates errors
 * that occur during processing. It also controls the loading mode (strict or lenient).
 * </p>
 *
 * @since 1.15.2
 */
@RequiredArgsConstructor
@ApiStatus.Internal
public final class ModelLoadContext {
    final @NotNull String name;
    final @NotNull ModelPlaceholder placeholder;
    final @NotNull ModelMeta meta;
    final @NotNull Map<String, ModelElement> elements;
    final @NotNull Map<String, ModelGroup> groups;
    final @NotNull Set<String> availableUUIDs;
    private final boolean strict;
    private final List<String> _errors = new ArrayList<>();
    final List<String> errors = Collections.unmodifiableList(_errors);

    /**
     * Tries to execute a supplier, catching exceptions in lenient mode.
     *
     * @param supplier the supplier to execute
     * @param fallbackFunction a function to provide a fallback value and error message on exception
     * @param <T> the return type
     * @return the result of the supplier or the fallback value
     * @since 1.15.2
     */
    @NotNull <T> T trySupply(@NotNull Supplier<T> supplier, @NotNull Function<Exception, Fallback<T>> fallbackFunction) {
        if (strict) return supplier.get();
        try {
            return supplier.get();
        } catch (Exception e) {
            var fallback = fallbackFunction.apply(e);
            _errors.add(fallback.message);
            return fallback.value;
        }
    }

    /**
     * Represents a fallback value and an associated error message.
     *
     * @param value the fallback value
     * @param message the error message
     * @param <T> the type of the value
     * @since 1.15.2
     */
    record Fallback<T>(@NotNull T value, @NotNull String message) {}
}
