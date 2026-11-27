/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.platform;

import org.jetbrains.annotations.NotNull;

import static java.lang.Math.fma;
import static java.lang.Math.sqrt;

/**
 * Represents a location in the underlying platform.
 * <p>
 * This interface provides access to coordinates, rotation, and the world,
 * as well as methods for manipulating the location.
 * </p>
 *
 * @since 2.0.0
 */
public interface PlatformLocation extends PlatformRegionHolder {

    /**
     * Returns the world associated with this location.
     * Synthetic locations, such as {@link PlatformAdapter#zero()}, may have no world.
     *
     * @return the associated world
     * @throws IllegalStateException if the location cannot resolve an associated world
     * @since 2.0.0
     */
    @NotNull PlatformWorld world();

    /**
     * Returns the X coordinate.
     *
     * @return the X coordinate
     * @since 2.0.0
     */
    double x();

    /**
     * Returns the Y coordinate.
     *
     * @return the Y coordinate
     * @since 2.0.0
     */
    double y();

    /**
     * Returns the Z coordinate.
     *
     * @return the Z coordinate
     * @since 2.0.0
     */
    double z();

    /**
     * Returns the pitch (vertical rotation).
     *
     * @return the pitch
     * @since 2.0.0
     */
    float pitch();

    /**
     * Returns the yaw (horizontal rotation).
     *
     * @return the yaw
     * @since 2.0.0
     */
    float yaw();

    /**
     * Creates a new location by adding the specified coordinates to this location.
     *
     * @param x the X offset
     * @param y the Y offset
     * @param z the Z offset
     * @return the new location with the same rotation and world association
     * @since 2.0.0
     */
    @NotNull PlatformLocation add(double x, double y, double z);

    /**
     * Calculates the Euclidean distance from the coordinates of both locations.
     *
     * @param other the other location
     * @return the distance
     * @since 2.1.0
     */
    default double distance(@NotNull PlatformLocation other) {
        return sqrt(distanceSquared(other));
    }

    /**
     * Calculates the squared Euclidean distance from the coordinates of both locations.
     * The calculation ignores the world and rotation of each location.
     *
     * @param other the other location
     * @return the squared distance
     * @since 2.1.0
     */
    default double distanceSquared(@NotNull PlatformLocation other) {
        var x = x() - other.x();
        var y = y() - other.y();
        var z = z() - other.z();
        return fma(x, x, fma(y, y, z * z));
    }
}
