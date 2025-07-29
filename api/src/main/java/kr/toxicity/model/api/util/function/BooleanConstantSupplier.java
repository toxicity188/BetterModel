package kr.toxicity.model.api.util.function;

import lombok.RequiredArgsConstructor;

import java.util.function.BooleanSupplier;

@RequiredArgsConstructor
public enum BooleanConstantSupplier implements BooleanSupplier {

    TRUE(true),
    FALSE(false)
    ;

    private final boolean value;

    @Override
    public boolean getAsBoolean() {
        return value;
    }
}
