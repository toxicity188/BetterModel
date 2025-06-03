package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.util.EntityUtil;
import kr.toxicity.model.api.util.FunctionUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Tracker's modifier
 * @param scale model scale
 * @param sightTrace use sight-trace
 * @param damageAnimation enables damage animation
 * @param damageTint enables damage tint
 * @param viewRange view range
 * @param shadow create shadow
 * @param hideOption hide option
 */
public record TrackerModifier(
        @NotNull Supplier<Float> scale,
        boolean sightTrace,
        boolean damageAnimation,
        boolean damageTint,
        float viewRange,
        boolean shadow,
        @NotNull HideOption hideOption
) {
    @lombok.Builder
    public record HideOption(
            boolean equipment,
            boolean fire,
            boolean visibility,
            boolean glowing
    ) {
        public static final HideOption DEFAULT = new HideOption(
                true,
                true,
                true,
                true
        );
    }

    /**
     * Default modifier
     */
    public static final TrackerModifier DEFAULT = new TrackerModifier(
            FunctionUtil.asSupplier(1F),
            true,
            true,
            true,
            EntityUtil.ENTITY_MODEL_VIEW_RADIUS,
            true,
            HideOption.DEFAULT
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
        private Supplier<Float> scale;
        private boolean sightTrace;
        private boolean damageAnimation;
        private boolean damageTint;
        private float viewRange;
        private boolean shadow;
        private HideOption hideOption;

        /**
         * Private initializer
         * @param modifier modifier
         */
        private Builder(@NotNull TrackerModifier modifier) {
            this.scale = modifier.scale;
            this.sightTrace = modifier.sightTrace;
            this.damageAnimation = modifier.damageAnimation;
            this.damageTint = modifier.damageTint;
            this.viewRange = modifier.viewRange;
            this.shadow = modifier.shadow;
            this.hideOption = modifier.hideOption;
        }

        /**
         * Sets scale
         * @param scale scale
         * @return self
         */
        public @NotNull Builder scale(@NotNull Supplier<Float> scale) {
            this.scale = Objects.requireNonNull(scale, "scale cannot be null.");
            return this;
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
         * Sets damage tint
         * @param damageTint damage tint
         * @return self
         */
        public @NotNull Builder damageTint(boolean damageTint) {
            this.damageTint = damageTint;
            return this;
        }

        /**
         * Sets view range
         * @param viewRange view range
         * @return self
         */
        public @NotNull Builder viewRange(float viewRange) {
            this.viewRange = viewRange;
            return this;
        }

        /**
         * Set shadow
         * @param shadow shadow
         * @return self
         */
        public @NotNull Builder shadow(boolean shadow) {
            this.shadow = shadow;
            return this;
        }

        /**
         * Set hide option
         * @param option hide option
         * @return self
         */
        public @NotNull Builder hideOption(@NotNull HideOption option) {
            this.hideOption = Objects.requireNonNull(option);
            return this;
        }

        /**
         * Builds modifier
         * @return modifier
         */
        public @NotNull TrackerModifier build() {
            return new TrackerModifier(
                    scale,
                    sightTrace,
                    damageAnimation,
                    damageTint,
                    viewRange,
                    shadow,
                    hideOption
            );
        }
    }
}
