package kr.toxicity.model.api.util.function;

import org.jetbrains.annotations.NotNull;

public record FloatConstantFunction<T>(@NotNull T value) implements FloatFunction<T> {
    @Override
    public @NotNull T applyAsFloat(float value) {
        return this.value;
    }
}
