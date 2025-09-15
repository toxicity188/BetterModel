/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.tracker;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

/**
 * Tracker's modifier
 * @param sightTrace use sight-trace
 * @param damageAnimation enables damage animation
 * @param damageTint enables damage tint
 */
public record TrackerModifier(
        @SerializedName("sight-trace") boolean sightTrace,
        @SerializedName("damage-animation") boolean damageAnimation,
        @SerializedName("damage-tint") boolean damageTint
) {
    /**
     * Default modifier
     */
    public static final TrackerModifier DEFAULT = new TrackerModifier(
            true,
            true,
            true
    );

    /**
     * Creates default builder
     * @return builder
     */
    public static @NotNull Builder builder() {
        return DEFAULT.toBuilder();
    }

    /**
     * Creates builder from original modifier.
     * @return builder
     */
    public @NotNull Builder toBuilder() {
        return new Builder(this);
    }

    /**
     * Builder class
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
         * Sets sight trace
         * @param sightTrace sight trace
         * @return self
         */
        public @NotNull Builder sightTrace(boolean sightTrace) {
            this.sightTrace = sightTrace;
            return this;
        }

        /**
         * Sets damage animation
         * @param damageAnimation damage animation
         * @return self
         */
        public @NotNull Builder damageAnimation(boolean damageAnimation) {
            this.damageAnimation = damageAnimation;
            return this;
        }

        /**
         * @deprecated no longer use. you have to follow {@link TrackerUpdateAction#viewRange(float)}
         * @param viewRange range
         * @return self
         */
        @Deprecated(forRemoval = true)
        public @NotNull Builder viewRange(float viewRange) {
            return this;
        }

        /**
         * Sets damage tint
         * @param damageTint damage tint
         * @return self
         */
        public @NotNull Builder damageTint(boolean damageTint) {
            this.damageTint = damageTint;
            return this;
        }

        /**
         * Builds modifier
         * @return modifier
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
