package kr.toxicity.model.api.util.function;

import org.jetbrains.annotations.NotNull;

public record FloatConstantSupplier(float value) implements FloatSupplier {

    public static final FloatConstantSupplier ONE = of(1F);

    public static @NotNull FloatConstantSupplier of(float value) {
        return new FloatConstantSupplier(value);
    }

    @Override
    public float getAsFloat() {
        return value;
    }
}
