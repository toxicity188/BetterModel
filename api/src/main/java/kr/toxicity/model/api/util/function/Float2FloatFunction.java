package kr.toxicity.model.api.util.function;

import org.jetbrains.annotations.NotNull;

/**
 * Float to float function
 */
@FunctionalInterface
public interface Float2FloatFunction {
    /**
     * Zero
     */
    Float2FloatConstantFunction ZERO = of(0);

    /**
     * Applies float value
     * @param value value
     * @return applied value
     */
    float applyAsFloat(float value);

    /**
     * Creates constant function by given value
     * @param value value
     * @return constant function
     */
    static @NotNull Float2FloatConstantFunction of(float value) {
        return new Float2FloatConstantFunction(value);
    }
}
