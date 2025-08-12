package kr.toxicity.model.api.util.function;

import org.jetbrains.annotations.NotNull;

/**
 * Float constant supplier
 * @param value value
 */
public record FloatConstantSupplier(float value) implements FloatSupplier {

    /**
     * One
     */
    public static final FloatConstantSupplier ONE = of(1F);

    /**
     * Creates supplier by given value
     * @param value val ue
     * @return supplier
     */
    public static @NotNull FloatConstantSupplier of(float value) {
        return new FloatConstantSupplier(value);
    }

    @Override
    public float getAsFloat() {
        return value;
    }
}
