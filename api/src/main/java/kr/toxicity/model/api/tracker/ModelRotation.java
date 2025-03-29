package kr.toxicity.model.api.tracker;

/**
 * Model rotation
 * @param x x-rot (pitch)
 * @param y y-rot (yaw)
 */
public record ModelRotation(float x, float y) {
    /**
     * Empty rotation
     */
    public static final ModelRotation EMPTY = new ModelRotation(0, 0);
}
