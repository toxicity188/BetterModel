package kr.toxicity.model.api.util.function;

import org.jetbrains.annotations.NotNull;

import java.util.function.BooleanSupplier;

public record BooleanConstantSupplier(boolean value) implements BooleanSupplier {

    public static final BooleanConstantSupplier TRUE = of(true);
    public static final BooleanConstantSupplier FALSE = of(false);

    public static @NotNull BooleanConstantSupplier of(boolean value) {
        return new BooleanConstantSupplier(value);
    }

    @Override
    public boolean getAsBoolean() {
        return value;
    }
}
