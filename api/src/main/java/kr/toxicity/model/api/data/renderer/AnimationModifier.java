package kr.toxicity.model.api.data.renderer;

import kr.toxicity.model.api.util.FunctionUtil;

import java.util.function.Supplier;

/**
 * A modifier of animation.
 */
public record AnimationModifier(Supplier<Boolean> predicate, int start, int end) {
    /**
     * Creates modifier
     *
     * @param predicate animation predicate
     * @param start     start time
     * @param end       end time
     */
    public AnimationModifier(Supplier<Boolean> predicate, int start, int end) {
        this.predicate = FunctionUtil.memoizeTick(predicate);
        this.start = start;
        this.end = end;
    }

    public static final AnimationModifier DEFAULT = new AnimationModifier(() -> true, 0, 0);
}
