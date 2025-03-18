package kr.toxicity.model.api.tracker;

public record ModelRotation(float x, float y) {
    public static final ModelRotation EMPTY = new ModelRotation(0, 0);
}
