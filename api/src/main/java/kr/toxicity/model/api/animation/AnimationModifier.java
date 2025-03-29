package kr.toxicity.model.api.animation;

import kr.toxicity.model.api.util.FunctionUtil;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * A modifier of animation.
 * @param predicate predicate
 * @param start start lerp
 * @param end end lerp
 * @param speed speed modifier
 */
public record AnimationModifier(@NotNull Supplier<Boolean> predicate, int start, int end, @NotNull SpeedModifier speed) {


    /**
     * Creates speed modifier
     * @param speed scala
     * @return speed modifier
     */
    public static @NotNull SpeedModifier speed(float speed) {
        return new SpeedModifier(speed);
    }

    /**
     * Creates speed modifier
     * @param supplier speed supplier
     * @return speed modifier
     */
    public static @NotNull SpeedModifier speed(@NotNull Supplier<Float> supplier) {
        return new SpeedModifier(supplier);
    }

    /**
     * Gets speed value
     * @return speed value
     */
    public float speedValue() {
        return speed.speed();
    }

    /**
     * A modifier of speed
     * @param supplier speed modifier
     */
    public record SpeedModifier(@NotNull Supplier<Float> supplier) {

        /**
         * Creates modifier
         * @param supplier speed modifier
         */
        public SpeedModifier(@NotNull Supplier<Float> supplier) {
            this.supplier = FunctionUtil.throttleTick(supplier);
        }

        /**
         * Creates modifier
         * @param speed speed
         */
        public SpeedModifier(float speed) {
            this(() -> speed);
        }

        /**
         * Gets speed
         * @return speed value
         */
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
