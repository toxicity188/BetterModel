package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.util.MathUtil;
import org.jetbrains.annotations.NotNull;

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
    /**
     * Invalid rotation
     */
    public static final ModelRotation INVALID = new ModelRotation(Float.MAX_VALUE, Float.MAX_VALUE);

    private static final float DEGREES_TO_PACKS = 256F / 360F;

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        return o instanceof ModelRotation other && packedX() == other.packedX() && packedY() == other.packedY();
    }

    @Override
    public int hashCode() {
        return ((Byte.hashCode(packedX()) & 0xFF) << 8) | (Byte.hashCode(packedY()) & 0xFF);
    }

    public @NotNull ModelRotation pitch() {
        return new ModelRotation(x, 0);
    }

    public @NotNull ModelRotation yaw() {
        return new ModelRotation(0, y);
    }

    public float radianX() {
        return x * MathUtil.DEGREES_TO_RADIANS;
    }

    public float radianY() {
        return y * MathUtil.DEGREES_TO_RADIANS;
    }

    public byte packedX() {
        return (byte) (x * DEGREES_TO_PACKS);
    }

    public byte packedY() {
        return (byte) (y * DEGREES_TO_PACKS);
    }
}
