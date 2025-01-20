package kr.toxicity.model.api.data.renderer;

import java.util.function.Supplier;

/**
 * A modifier of animation.
 * @param predicate animation predicate
 * @param start start time
 * @param end end time
 */
public record AnimationModifier(Supplier<Boolean> predicate, int start, int end) {
    public static final AnimationModifier DEFAULT = new AnimationModifier(() -> true, 0, 0);
}
