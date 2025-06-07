package kr.toxicity.model.api.animation;

import kr.toxicity.model.api.util.function.FloatSupplier;
import kr.toxicity.model.api.util.FunctionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;

/**
 * A modifier of animation.
 * @param predicate predicate
 * @param start start lerp
 * @param end end lerp
 * @param type animation type
 * @param speed speed modifier
 */
public record AnimationModifier(@NotNull BooleanSupplier predicate, int start, int end, @Nullable AnimationIterator.Type type, @NotNull SpeedModifier speed) {


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
    public static @NotNull SpeedModifier speed(@NotNull FloatSupplier supplier) {
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
    public record SpeedModifier(@NotNull FloatSupplier supplier) {

        /**
         * Creates modifier
         * @param supplier speed modifier
         */
        public SpeedModifier(@NotNull FloatSupplier supplier) {
            this.supplier = FunctionUtil.throttleTickFloat(supplier);
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
     * @param start     start time
     * @param end       end time
     * @param speed     speed
     */
    public AnimationModifier(int start, int end, float speed) {
        this(() -> true, start, end, null, speed);
    }

    /**
     * Creates modifier
     *
     * @param predicate animation predicate
     * @param start     start time
     * @param end       end time
     * @param speed     speed
     */
    public AnimationModifier(@NotNull BooleanSupplier predicate, int start, int end, float speed) {
        this(predicate, start, end, null, speed(speed));
    }

    /**
     * Creates modifier
     *
     * @param predicate animation predicate
     * @param start     start time
     * @param end       end time
     * @param supplier     speed supplier
     */
    public AnimationModifier(@NotNull BooleanSupplier predicate, int start, int end, @NotNull FloatSupplier supplier) {
        this(predicate, start, end, null, new SpeedModifier(supplier));
    }

    /**
     * Creates modifier
     *
     * @param predicate animation predicate
     * @param start     start time
     * @param end       end time
     * @param type type
     * @param speed     speed
     */
    public AnimationModifier(@NotNull BooleanSupplier predicate, int start, int end, @Nullable AnimationIterator.Type type, float speed) {
        this(predicate, start, end, type, speed(speed));
    }


    /**
     * Creates modifier
     *
     * @param predicate animation predicate
     * @param start     start time
     * @param end       end time
     * @param type type
     * @param speed     speed
     */
    public AnimationModifier(@NotNull BooleanSupplier predicate, int start, int end, @Nullable AnimationIterator.Type type, @NotNull FloatSupplier speed) {
        this(predicate, start, end, type, new SpeedModifier(speed));
    }

    /**
     * Creates modifier
     *
     * @param predicate animation predicate
     * @param start     start time
     * @param end       end time
     * @param type type
     * @param speed     speed
     */
    public AnimationModifier(@NotNull BooleanSupplier predicate, int start, int end, @Nullable AnimationIterator.Type type, @NotNull SpeedModifier speed) {
        this.predicate = FunctionUtil.throttleTickBoolean(predicate);
        this.start = start;
        this.end = end;
        this.type = type;
        this.speed = speed;
    }

    public static final AnimationModifier DEFAULT = new AnimationModifier(1, 0, 1F);
    public static final AnimationModifier DEFAULT_WITH_PLAY_ONCE = new AnimationModifier(() -> true, 1, 0, AnimationIterator.Type.PLAY_ONCE, 1F);

    /**
     * Gets modifier's type or default value
     * @param defaultType default value
     * @return modifier's type or default value
     */
    public @NotNull AnimationIterator.Type type(@NotNull AnimationIterator.Type defaultType) {
        return type != null ? type : defaultType;
    }
}
