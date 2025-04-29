package kr.toxicity.model.api.tracker;

import java.util.function.Supplier;

public interface ModelRotator extends Supplier<ModelRotation> {
    ModelRotator EMPTY = () -> ModelRotation.EMPTY;
}
