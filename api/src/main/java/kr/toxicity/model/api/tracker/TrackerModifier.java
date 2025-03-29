package kr.toxicity.model.api.tracker;

/**
 * Tracker's modifier
 * @param scale model scale
 * @param sightTrace use sight-trace
 * @param damageEffect enables damage effect
 */
public record TrackerModifier(
        float scale,
        boolean sightTrace,
        boolean damageEffect
) {
    /**
     * Default modifier
     */
    public static final TrackerModifier DEFAULT = new TrackerModifier(
            1F,
            true,
            true
    );
}
