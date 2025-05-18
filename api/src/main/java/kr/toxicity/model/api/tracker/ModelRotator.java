package kr.toxicity.model.api.tracker;

import java.util.function.Supplier;

/**
 * Model rotator
 */
public interface ModelRotator extends Supplier<ModelRotation> {
    /**
     * Empty rotator
     */
    ModelRotator EMPTY = () -> ModelRotation.EMPTY;
}
