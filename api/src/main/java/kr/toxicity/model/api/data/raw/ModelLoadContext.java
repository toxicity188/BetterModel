/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
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
 * Load context
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

    record Fallback<T>(@NotNull T value, @NotNull String message) {}
}
