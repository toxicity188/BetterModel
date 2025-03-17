package kr.toxicity.model.api.tracker;

public record TrackerModifier(
        float scale,
        boolean sightTrace,
        boolean damageEffect
) {
    public static final TrackerModifier DEFAULT = new TrackerModifier(
            1F,
            true,
            true
    );
}
