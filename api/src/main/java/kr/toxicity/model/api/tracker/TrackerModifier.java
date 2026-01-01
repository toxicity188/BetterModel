/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.tracker;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

/**
 * Configuration options for a {@link Tracker}.
 * <p>
 * This record controls various behaviors such as visibility checks (sight trace),
 * automatic damage animations, and damage tinting effects.
 * </p>
 *
 * @param sightTrace whether to perform sight tracing for visibility
 * @param damageAnimation whether to play automatic damage animations
 * @param damageTint whether to apply a red tint when damaged
 * @since 1.15.2
 */
public record TrackerModifier(
    @SerializedName("sight-trace") boolean sightTrace,
    @SerializedName("damage-animation") boolean damageAnimation,
    @SerializedName("damage-tint") boolean damageTint
) {
    /**
     * The default modifier configuration (all enabled).
     * @since 1.15.2
     */
    public static final TrackerModifier DEFAULT = new TrackerModifier(
        true,
        true,
        true
    );

    /**
     * Creates a new builder initialized with default values.
     *
     * @return a new builder
     * @since 1.15.2
     */
    public static @NotNull Builder builder() {
        return DEFAULT.toBuilder();
    }

    /**
     * Creates a new builder initialized with this modifier's values.
     *
     * @return a new builder
     * @since 1.15.2
     */
    public @NotNull Builder toBuilder() {
        return new Builder(this);
    }

    /**
     * Builder for {@link TrackerModifier}.
     *
     * @since 1.15.2
     */
    public static final class Builder {
        private boolean sightTrace;
        private boolean damageAnimation;
        private boolean damageTint;

        /**
         * Private initializer
         * @param modifier modifier
         */
        private Builder(@NotNull TrackerModifier modifier) {
            this.sightTrace = modifier.sightTrace;
            this.damageAnimation = modifier.damageAnimation;
            this.damageTint = modifier.damageTint;
        }

        /**
         * Sets whether to use sight tracing.
         *
         * @param sightTrace true to enable sight tracing
         * @return this builder
         * @since 1.15.2
         */
        public @NotNull Builder sightTrace(boolean sightTrace) {
            this.sightTrace = sightTrace;
            return this;
        }

        /**
         * Sets whether to enable damage animations.
         *
         * @param damageAnimation true to enable damage animations
         * @return this builder
         * @since 1.15.2
         */
        public @NotNull Builder damageAnimation(boolean damageAnimation) {
            this.damageAnimation = damageAnimation;
            return this;
        }

        /**
         * Sets whether to enable damage tinting.
         *
         * @param damageTint true to enable damage tinting
         * @return this builder
         * @since 1.15.2
         */
        public @NotNull Builder damageTint(boolean damageTint) {
            this.damageTint = damageTint;
            return this;
        }

        /**
         * Builds the {@link TrackerModifier}.
         *
         * @return the created modifier
         * @since 1.15.2
         */
        public @NotNull TrackerModifier build() {
            return new TrackerModifier(
                sightTrace,
                damageAnimation,
                damageTint
            );
        }
    }
}
