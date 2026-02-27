/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.util.function;

/**
 * Float constant supplier
 * @param value value
 */
public record FloatConstantSupplier(float value) implements FloatSupplier {

    /**
     * One
     */
    public static final FloatConstantSupplier ONE = FloatSupplier.of(1F);

    @Override
    public float getAsFloat() {
        return value;
    }
}
