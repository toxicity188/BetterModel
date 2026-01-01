/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.util.MathUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the rotation of a model in degrees.
 * <p>
 * This record stores pitch (x) and yaw (y) values and provides utility methods for conversion.
 * </p>
 *
 * @param x the pitch (x-rotation) in degrees
 * @param y the yaw (y-rotation) in degrees
 * @since 1.15.2
 */
public record ModelRotation(float x, float y) {
    /**
     * A rotation of (0, 0).
     * @since 1.15.2
     */
    public static final ModelRotation EMPTY = new ModelRotation(0, 0);
    /**
     * An invalid rotation value used for initialization or error states.
     * @since 1.15.2
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
     * Returns a new rotation with only the pitch component.
     *
     * @return the pitch-only rotation
     * @since 1.15.2
     */
    public @NotNull ModelRotation pitch() {
        return new ModelRotation(x, 0);
    }

    /**
     * Returns a new rotation with only the yaw component.
     *
     * @return the yaw-only rotation
     * @since 1.15.2
     */
    public @NotNull ModelRotation yaw() {
        return new ModelRotation(0, y);
    }

    /**
     * Returns the pitch in radians.
     *
     * @return the pitch in radians
     * @since 1.15.2
     */
    public float radianX() {
        return x * MathUtil.DEGREES_TO_RADIANS;
    }

    /**
     * Returns the yaw in radians.
     *
     * @return the yaw in radians
     * @since 1.15.2
     */
    public float radianY() {
        return y * MathUtil.DEGREES_TO_RADIANS;
    }

    /**
     * Returns the pitch packed as a byte (Minecraft protocol format).
     *
     * @return the packed pitch
     * @since 1.15.2
     */
    public byte packedX() {
        return (byte) (x * MathUtil.DEGREES_TO_PACKED_BYTE);
    }

    /**
     * Returns the yaw packed as a byte (Minecraft protocol format).
     *
     * @return the packed yaw
     * @since 1.15.2
     */
    public byte packedY() {
        return (byte) (y * MathUtil.DEGREES_TO_PACKED_BYTE);
    }
}
