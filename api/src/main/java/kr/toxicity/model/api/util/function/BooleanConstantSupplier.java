package kr.toxicity.model.api.util.function;

import lombok.RequiredArgsConstructor;

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

    @Override
    public boolean getAsBoolean() {
        return value;
    }
}
