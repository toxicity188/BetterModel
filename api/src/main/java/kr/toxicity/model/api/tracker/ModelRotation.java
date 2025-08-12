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

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        return o instanceof ModelRotation other && packedX() == other.packedX() && packedY() == other.packedY();
    }

    @Override
    public int hashCode() {
        return ((Byte.hashCode(packedX()) & 0xFF) << 8) | (Byte.hashCode(packedY()) & 0xFF);
    }

    /**
     * Gets pitch value
     * @return pitch
     */
    public @NotNull ModelRotation pitch() {
        return new ModelRotation(x, 0);
    }

    /**
     * Gets yaw value
     * @return yaw value
     */
    public @NotNull ModelRotation yaw() {
        return new ModelRotation(0, y);
    }

    /**
     * Gets radian x
     * @return radian x
     */
    public float radianX() {
        return x * MathUtil.DEGREES_TO_RADIANS;
    }

    /**
     * Gets radian y
     * @return radian y
     */
    public float radianY() {
        return y * MathUtil.DEGREES_TO_RADIANS;
    }

    /**
     * Gets packed x
     * @return packed x
     */
    public byte packedX() {
        return (byte) (x * MathUtil.DEGREES_TO_PACKED_BYTE);
    }

    /**
     * Gets packed y
     * @return packed y
     */
    public byte packedY() {
        return (byte) (y * MathUtil.DEGREES_TO_PACKED_BYTE);
    }
}
