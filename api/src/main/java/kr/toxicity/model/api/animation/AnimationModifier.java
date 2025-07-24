package kr.toxicity.model.api.animation;

import kr.toxicity.model.api.util.function.BooleanConstantSupplier;
import kr.toxicity.model.api.util.function.FloatConstantSupplier;
import kr.toxicity.model.api.util.function.FloatSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * A modifier of animation.
 * @param predicate predicate
 * @param start start lerp
 * @param end end lerp
 * @param type animation type
 * @param speed speed modifier
 * @param override override
 */
public record AnimationModifier(
        @NotNull BooleanSupplier predicate,
        int start,
        int end,
        @Nullable AnimationIterator.Type type,
        @NotNull FloatSupplier speed,
        @Nullable Boolean override
) {

    /**
     * Default modifier
     */
    public static final AnimationModifier DEFAULT = builder().build();

    /**
     * Default with play once modifier
     */
    public static final AnimationModifier DEFAULT_WITH_PLAY_ONCE = builder().type(AnimationIterator.Type.PLAY_ONCE).build();

    public static @NotNull Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private BooleanSupplier predicate = BooleanConstantSupplier.TRUE;
        private int start = 1;
        private int end = 0;
        private AnimationIterator.Type type = null;
        private FloatSupplier speed = FloatConstantSupplier.ONE;
        private Boolean override = false;

        private Builder() {
        }

        public @NotNull Builder predicate(@NotNull BooleanSupplier predicate) {
            this.predicate = Objects.requireNonNull(predicate);
            return this;
        }

        public @NotNull Builder start(int start) {
            this.start = start;
            return this;
        }

        public @NotNull Builder end(int end) {
            this.end = end;
            return this;
        }

        public @NotNull Builder type(@Nullable AnimationIterator.Type type) {
            this.type = type;
            return this;
        }

        public @NotNull Builder speed(@NotNull FloatSupplier speed) {
            this.speed = Objects.requireNonNull(speed);
            return this;
        }

        public @NotNull Builder override(@Nullable Boolean override) {
            this.override = override;
            return this;
        }

        public @NotNull AnimationModifier build() {
            return new AnimationModifier(
                    predicate,
                    start,
                    end,
                    type,
                    speed,
                    override
            );
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
        this(BooleanConstantSupplier.TRUE, start, end, null, FloatConstantSupplier.of(speed));
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
        this(predicate, start, end, null, FloatConstantSupplier.of(speed));
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
        this(predicate, start, end, null, supplier);
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
        this(predicate, start, end, type, FloatConstantSupplier.of(speed), null);
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
        this(predicate, start, end, type, speed, null);
    }

    /**
     * Gets modifier's type or default value
     * @param defaultType default value
     * @return modifier's type or default value
     */
    public @NotNull AnimationIterator.Type type(@NotNull AnimationIterator.Type defaultType) {
        return type != null ? type : defaultType;
    }

    /**
     * Gets speed value
     * @return speed value
     */
    public float speedValue() {
        return speed.getAsFloat();
    }

    /**
     * Gets override
     * @param original original value
     * @return override
     */
    public boolean override(boolean original) {
        return override != null ? override : original;
    }
}
