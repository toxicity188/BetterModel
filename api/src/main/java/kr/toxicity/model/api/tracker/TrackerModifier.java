package kr.toxicity.model.api.tracker;

import com.google.gson.annotations.SerializedName;
import kr.toxicity.model.api.util.EntityUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Tracker's modifier
 * @param sightTrace use sight-trace
 * @param damageAnimation enables damage animation
 * @param damageTint enables damage tint
 * @param viewRange view range
 */
public record TrackerModifier(
        @SerializedName("sight-trace") boolean sightTrace,
        @SerializedName("damage-animation") boolean damageAnimation,
        @SerializedName("damage-tint") boolean damageTint,
        @SerializedName("view-range") float viewRange
) {
    /**
     * Default modifier
     */
    public static final TrackerModifier DEFAULT = new TrackerModifier(
            true,
            true,
            true,
            EntityUtil.ENTITY_MODEL_VIEW_RADIUS
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
        private float viewRange;

        /**
         * Private initializer
         * @param modifier modifier
         */
        private Builder(@NotNull TrackerModifier modifier) {
            this.sightTrace = modifier.sightTrace;
            this.damageAnimation = modifier.damageAnimation;
            this.damageTint = modifier.damageTint;
            this.viewRange = modifier.viewRange;
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
         * Builds modifier
         * @return modifier
         */
        public @NotNull TrackerModifier build() {
            return new TrackerModifier(
                    sightTrace,
                    damageAnimation,
                    damageTint,
                    viewRange
            );
        }
    }
}
