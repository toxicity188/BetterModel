package kr.toxicity.model.api.util.function;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.function.BooleanSupplier;

/**
 * Boolean constant supplier
 */
@RequiredArgsConstructor
public enum BooleanConstantSupplier implements BooleanSupplier {
    /**
     * True
     */
    TRUE(true),
    /**
     * False
     */
    FALSE(false)
    ;

    private final boolean value;

    public static @NotNull BooleanConstantSupplier of(boolean value) {
        return value ? TRUE : FALSE;
    }

    @Override
    public boolean getAsBoolean() {
        return value;
    }
}
