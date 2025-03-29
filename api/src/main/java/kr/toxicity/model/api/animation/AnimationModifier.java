package kr.toxicity.model.api.animation;

import kr.toxicity.model.api.util.FunctionUtil;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * A modifier of animation.
 */
public record AnimationModifier(@NotNull Supplier<Boolean> predicate, int start, int end, @NotNull SpeedModifier speed) {


    public static @NotNull SpeedModifier speed(float speed) {
        return new SpeedModifier(speed);
    }

    public static @NotNull SpeedModifier speed(@NotNull Supplier<Float> supplier) {
        return new SpeedModifier(supplier);
    }

    public float speedValue() {
        return speed.speed();
    }

    public record SpeedModifier(@NotNull Supplier<Float> supplier) {

        public SpeedModifier(@NotNull Supplier<Float> supplier) {
            this.supplier = FunctionUtil.throttleTick(supplier);
        }

        public SpeedModifier(float speed) {
            this(() -> speed);
        }

        public float speed() {
            return supplier.get();
        }
    }

    /**
     * Creates modifier
     *
     * @param predicate animation predicate
     * @param start     start time
     * @param end       end time
     * @param speed     speed
     */
    public AnimationModifier(@NotNull Supplier<Boolean> predicate, int start, int end, float speed) {
        this(predicate, start, end, speed(speed));
    }

    /**
     * Creates modifier
     *
     * @param predicate animation predicate
     * @param start     start time
     * @param end       end time
     * @param supplier     speed supplier
     */
    public AnimationModifier(@NotNull Supplier<Boolean> predicate, int start, int end, @NotNull Supplier<Float> supplier) {
        this(predicate, start, end, new SpeedModifier(supplier));
    }


    /**
     * Creates modifier
     *
     * @param predicate animation predicate
     * @param start     start time
     * @param end       end time
     * @param speed     speed
     */
    public AnimationModifier(@NotNull Supplier<Boolean> predicate, int start, int end, @NotNull SpeedModifier speed) {
        this.predicate = FunctionUtil.throttleTick(predicate);
        this.start = start;
        this.end = end;
        this.speed = speed;
    }

    public static final AnimationModifier DEFAULT = new AnimationModifier(() -> true, 1, 0, 1F);
    public static final AnimationModifier DEFAULT_LOOP = new AnimationModifier(() -> true, 4, 0, 1F);
}
