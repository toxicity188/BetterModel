package kr.toxicity.model.api.tracker;

public record TrackerModifier(
        float scale,
        boolean sightTrace
) {
    public static final TrackerModifier DEFAULT = new TrackerModifier(
            1F,
            true
    );
}
