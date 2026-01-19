/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.platform;

import org.jetbrains.annotations.NotNull;

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
     *
     * @return the world
     * @since 2.0.0
     */
    PlatformWorld world();

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
     * @return the new location
     * @since 2.0.0
     */
    @NotNull PlatformLocation add(double x, double y, double z);
}
