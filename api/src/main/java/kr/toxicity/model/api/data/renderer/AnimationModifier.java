package kr.toxicity.model.api.data.renderer;

import kr.toxicity.model.api.util.FunctionUtil;

import java.util.function.Supplier;

/**
 * A modifier of animation.
 */
public record AnimationModifier(Supplier<Boolean> predicate, int start, int end, float speed) {
    /**
     * Creates modifier
     *
     * @param predicate animation predicate
     * @param start     start time
     * @param end       end time
     * @param speed     speed
     */
    public AnimationModifier(Supplier<Boolean> predicate, int start, int end, float speed) {
        this.predicate = FunctionUtil.memoizeTick(predicate);
        this.start = start;
        this.end = end;
        this.speed = speed;
    }

    public static final AnimationModifier DEFAULT = new AnimationModifier(() -> true, 1, 0, 1F);
    public static final AnimationModifier DEFAULT_LOOP = new AnimationModifier(() -> true, 4, 0, 1F);
}
