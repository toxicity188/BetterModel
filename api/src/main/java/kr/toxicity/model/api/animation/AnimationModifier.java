/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.animation;

import kr.toxicity.model.api.util.MathUtil;
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
        @Nullable BooleanSupplier predicate,
        int start,
        int end,
        @Nullable AnimationIterator.Type type,
        @Nullable FloatSupplier speed,
        @Nullable Boolean override,
        @Nullable Player player
) {

    /**
     * Default modifier
     */
    @NotNull
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
     * Makes this modifier as builder
     * @return builder
     */
    public @NotNull Builder toBuilder() {
        return builder()
                .predicate(predicate)
                .start(start)
                .end(end)
                .type(type)
                .speed(speed)
                .override(override)
                .player(player);
    }

    /**
     * Builder
     */
    public static final class Builder {
        private BooleanSupplier predicate = null;
        private int start = 1;
        private int end = 0;
        private AnimationIterator.Type type = null;
        private FloatSupplier speed = null;
        private Boolean override = null;
        private Player player = null;

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
        public @NotNull Builder predicate(@Nullable BooleanSupplier predicate) {
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
         * @param speed speed
         * @return self
         */
        public @NotNull Builder speed(float speed) {
            this.speed = toSupplier(speed);
            return this;
        }

        /**
         * Sets the speed modifier of this modifier
         * @param speed speed modifier
         * @return self
         */
        public @NotNull Builder speed(@Nullable FloatSupplier speed) {
            this.speed = speed;
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
         * Merges non-default value with other modifier
         * @param modifier modifier
         * @return self
         */
        public @NotNull Builder mergeNotDefault(@NotNull AnimationModifier modifier) {
            if (modifier.predicate != null) predicate(modifier.predicate);
            if (modifier.start >= 0) start(modifier.start);
            if (modifier.end >= 0) end(modifier.end);
            if (modifier.type != null) type(modifier.type);
            if (modifier.speed != null) speed(modifier.speed);
            if (modifier.override != null) override(modifier.override);
            if (modifier.player != null) player(modifier.player);
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
     */
    public AnimationModifier(int start, int end) {
        this(start, end, null, null);
    }

    /**
     * Creates modifier
     *
     * @param start     start time
     * @param end       end time
     * @param speedValue  speed value
     */
    public AnimationModifier(int start, int end, float speedValue) {
        this(start, end, null, FloatConstantSupplier.of(speedValue));
    }

    /**
     * Creates modifier
     *
     * @param start     start time
     * @param end       end time
     * @param supplier  speed supplier
     */
    public AnimationModifier(int start, int end, @Nullable FloatSupplier supplier) {
        this(start, end, null, supplier);
    }

    /**
     * Creates modifier
     *
     * @param start     start time
     * @param end       end time
     * @param type      type
     */
    public AnimationModifier(int start, int end, @Nullable AnimationIterator.Type type) {
        this(start, end, type, null);
    }

    /**
     * Creates modifier
     *
     * @param start     start time
     * @param end       end time
     * @param type type
     * @param speed     speed
     */
    public AnimationModifier(int start, int end, @Nullable AnimationIterator.Type type, @Nullable FloatSupplier speed) {
        this(null, start, end, type, speed);
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
    public AnimationModifier(@Nullable BooleanSupplier predicate, int start, int end, @Nullable AnimationIterator.Type type, @Nullable FloatSupplier speed) {
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
        return speed != null ? speed.getAsFloat() : 1F;
    }

    /**
     * Gets predicate value
     * @return predicate value
     */
    public boolean predicateValue() {
        return predicate == null || predicate.getAsBoolean();
    }

    /**
     * Gets override
     * @param original original value
     * @return override
     */
    public boolean override(boolean original) {
        return override != null ? override : original;
    }

    private static @Nullable FloatConstantSupplier toSupplier(float speed) {
        return MathUtil.isSimilar(speed, 1F) ? null : FloatConstantSupplier.of(speed);
    }
}
