package kr.toxicity.model.api.tracker;

import java.util.Objects;

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

    private static final float DEGREE_TO_PACK = 256F / 360F;

    @Override
    public boolean equals(Object o) {
        return o instanceof ModelRotation other && packedX() == other.packedX() && packedY() == other.packedY();
    }

    @Override
    public int hashCode() {
        return Objects.hash(packedX(), packedY());
    }

    public byte packedX() {
        return (byte) (x * DEGREE_TO_PACK);
    }

    public byte packedY() {
        return (byte) (y * DEGREE_TO_PACK);
    }
}
