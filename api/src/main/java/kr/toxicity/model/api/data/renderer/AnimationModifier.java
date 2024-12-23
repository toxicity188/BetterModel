package kr.toxicity.model.api.data.renderer;

import java.util.function.Supplier;

public record AnimationModifier(Supplier<Boolean> predicate, int start, int end) {
    public static final AnimationModifier DEFAULT = new AnimationModifier(() -> true, 0, 0);
}
