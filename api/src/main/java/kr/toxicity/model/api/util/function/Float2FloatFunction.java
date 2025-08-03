package kr.toxicity.model.api.util.function;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface Float2FloatFunction {
    Float2FloatConstantFunction ZERO = of(0);
    float applyAsFloat(float value);

    static @NotNull Float2FloatConstantFunction of(float value) {
        return new Float2FloatConstantFunction(value);
    }
}
