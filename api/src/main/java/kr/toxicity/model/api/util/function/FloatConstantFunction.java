/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.util.function;

import org.jetbrains.annotations.NotNull;

/**
 * Float constant function
 * @param value value
 * @param <T> type
 */
public record FloatConstantFunction<T>(@NotNull T value) implements FloatFunction<T> {
    @Override
    public @NotNull T apply(float value) {
        return this.value;
    }
}
