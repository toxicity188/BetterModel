package kr.toxicity.model.api.animation;

import kr.toxicity.model.api.util.function.BooleanConstantSupplier;
import kr.toxicity.model.api.util.function.FloatConstantSupplier;
import kr.toxicity.model.api.util.function.FloatSupplier;
import org.bukkit.entity.Player;
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
 * @param player player
 */
public record AnimationModifier(
        @NotNull BooleanSupplier predicate,
        int start,
        int end,
        @Nullable AnimationIterator.Type type,
        @NotNull FloatSupplier speed,
        @Nullable Boolean override,
        @Nullable Player player
) {

    /**
     * Default modifier
     */
    public static final AnimationModifier DEFAULT = builder().build();

    /**
     * Default with play once modifier
     */
    public static final AnimationModifier DEFAULT_WITH_PLAY_ONCE = builder().type(AnimationIterator.Type.PLAY_ONCE).build();

    /**
     * Creates builder
     * @return builder
     */
    public static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * Builder
     */
    public static final class Builder {
        private BooleanSupplier predicate = BooleanConstantSupplier.TRUE;
        private int start = 1;
        private int end = 0;
        private AnimationIterator.Type type = null;
        private FloatSupplier speed = FloatConstantSupplier.ONE;
        private Boolean override = null;
        private Player player;

        /**
         * Private initializer
         */
        private Builder() {
        }

        /**
         * Sets the predicate of this modifier
         * @param predicate predicate
         * @return self
         */
        public @NotNull Builder predicate(@NotNull BooleanSupplier predicate) {
            this.predicate = Objects.requireNonNull(predicate);
            return this;
        }

        /**
         * Sets the lerp-in time of this modifier
         * @param start lerp-in time
         * @return self
         */
        public @NotNull Builder start(int start) {
            this.start = start;
            return this;
        }

        /**
         * Sets the lerp-out time of this modifier
         * @param end lerp-out time
         * @return self
         */
        public @NotNull Builder end(int end) {
            this.end = end;
            return this;
        }

        /**
         * Sets the animation type of this modifier
         * @param type animation type
         * @return self
         */
        public @NotNull Builder type(@Nullable AnimationIterator.Type type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the speed modifier of this modifier
         * @param speed speed modifier
         * @return self
         */
        public @NotNull Builder speed(@NotNull FloatSupplier speed) {
            this.speed = Objects.requireNonNull(speed);
            return this;
        }

        /**
         * Sets the override flag of this modifier
         * @param override override flag
         * @return self
         */
        public @NotNull Builder override(@Nullable Boolean override) {
            this.override = override;
            return this;
        }

        /**
         * Sets the target player of this modifier
         * @param player target player
         * @return self
         */
        public @NotNull Builder player(@Nullable Player player) {
            this.player = player;
            return this;
        }

        /**
         * Builds animation modifier
         * @return build
         */
        public @NotNull AnimationModifier build() {
            return new AnimationModifier(
                    predicate,
                    start,
                    end,
                    type,
                    speed,
                    override,
                    player
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
        this(predicate, start, end, type, FloatConstantSupplier.of(speed), null, null);
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
        this(predicate, start, end, type, speed, null, null);
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
