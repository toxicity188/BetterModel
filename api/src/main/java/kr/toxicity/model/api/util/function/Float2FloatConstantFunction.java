package kr.toxicity.model.api.util.function;

public record Float2FloatConstantFunction(float value) implements Float2FloatFunction {
    @Override
    public float applyAsFloat(float value) {
        return this.value;
    }
}
