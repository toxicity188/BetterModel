package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.util.EntityUtil;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Tracker's modifier
 * @param scale model scale
 * @param sightTrace use sight-trace
 * @param damageEffect enables damage effect
 */
public record TrackerModifier(
        @NotNull Supplier<Float> scale,
        boolean sightTrace,
        boolean damageEffect,
        float viewRange,
        boolean shadow
) {

    /**
     * Default modifier
     */
    public static final TrackerModifier DEFAULT = new TrackerModifier(
            () -> 1F,
            true,
            true,
            EntityUtil.ENTITY_MODEL_VIEW_RADIUS,
            true
    );

    /**
     * Creates builder
     * @return builder
     */
    public static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * Builder class
     */
    public static final class Builder {
        private Supplier<Float> scale = DEFAULT.scale;
        private boolean sightTrace = DEFAULT.sightTrace;
        private boolean damageEffect = DEFAULT.damageEffect;
        private float viewRange = DEFAULT.viewRange;
        private boolean shadow = DEFAULT.shadow;

        /**
         * Private initializer
         */
        private Builder() {
        }

        /**
         * Sets scale
         * @param scale scale
         * @return self
         */
        public @NotNull Builder scale(Supplier<Float> scale) {
            this.scale = scale;
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
         * Sets damage effect
         * @param damageEffect damage effect
         * @return self
         */
        public @NotNull Builder damageEffect(boolean damageEffect) {
            this.damageEffect = damageEffect;
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
         * Sets shadow
         * @param shadow shadow
         * @return self
         */
        public @NotNull Builder shadow(boolean shadow) {
            this.shadow = shadow;
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
                    damageEffect,
                    viewRange,
                    shadow
            );
        }
    }
}
