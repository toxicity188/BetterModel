/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.util.function;

/**
 * Float to float constant function
 * @param value value
 */
public record Float2FloatConstantFunction(float value) implements Float2FloatFunction {
    @Override
    public float applyAsFloat(float value) {
        return this.value;
    }
}
