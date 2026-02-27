/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.util.function;

import org.jetbrains.annotations.NotNull;

/**
 * Float supplier
 */
@FunctionalInterface
public interface FloatSupplier {
    /**
     * Gets float value
     * @return float value
     */
    float getAsFloat();

    /**
     * Creates supplier by given value
     * @param value val ue
     * @return supplier
     */
    static @NotNull FloatConstantSupplier of(float value) {
        return new FloatConstantSupplier(value);
    }
}
