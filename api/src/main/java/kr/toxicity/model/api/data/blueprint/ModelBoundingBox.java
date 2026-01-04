/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.blueprint;

import kr.toxicity.model.api.bone.BoneName;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaterniond;
import org.joml.Vector3d;

/**
 * Represents an axis-aligned bounding box (AABB) for a model part.
 * <p>
 * This record defines the spatial extent of a model element or group, used for hitboxes and collision detection.
 * </p>
 *
 * @param minX the minimum X coordinate
 * @param minY the minimum Y coordinate
 * @param minZ the minimum Z coordinate
 * @param maxX the maximum X coordinate
 * @param maxY the maximum Y coordinate
 * @param maxZ the maximum Z coordinate
 * @since 1.15.2
 */
public record ModelBoundingBox(
    double minX,
    double minY,
    double minZ,
    double maxX,
    double maxY,
    double maxZ
) {
    /**
     * A minimal bounding box size (0.1 x 0.1 x 0.1).
     * @since 1.15.2
     */
    public static final ModelBoundingBox MIN = of(0.1, 0.1, 0.1);

    /**
     * Creates a bounding box from two corner vectors.
     *
     * @param min the minimum corner vector
     * @param max the maximum corner vector
     * @return the bounding box
     * @since 1.15.2
     */
    public static @NotNull ModelBoundingBox of(@NotNull Vector3d min, @NotNull Vector3d max) {
        return of(
            min.x,
            min.y,
            min.z,
            max.x,
            max.y,
            max.z
        );
    }

    /**
     * Creates a bounding box centered at the origin with the given dimensions.
     *
     * @param x the width (X-axis)
     * @param y the height (Y-axis)
     * @param z the depth (Z-axis)
     * @return the centered bounding box
     * @since 1.15.2
     */
    public static @NotNull ModelBoundingBox of(double x, double y, double z) {
        return of(
            -x / 2,
            -y / 2,
            -z / 2,
            x / 2,
            y / 2,
            z / 2
        );
    }

    /**
     * Creates a bounding box from explicit min/max coordinates.
     * <p>
     * This method automatically ensures that min values are less than or equal to max values.
     * </p>
     *
     * @param minX the first X coordinate
     * @param minY the first Y coordinate
     * @param minZ the first Z coordinate
     * @param maxX the second X coordinate
     * @param maxY the second Y coordinate
     * @param maxZ the second Z coordinate
     * @return the normalized bounding box
     * @since 1.15.2
     */
    public static @NotNull ModelBoundingBox of(
        double minX,
        double minY,
        double minZ,
        double maxX,
        double maxY,
        double maxZ
    ) {
        return new ModelBoundingBox(
            Math.min(minX, maxX),
            Math.min(minY, maxY),
            Math.min(minZ, maxZ),
            Math.max(minX, maxX),
            Math.max(minY, maxY),
            Math.max(minZ, maxZ)
        );
    }

    /**
     * Associates this bounding box with a bone name.
     *
     * @param name the bone name
     * @return a {@link NamedBoundingBox} wrapping this box
     * @since 1.15.2
     */
    public @NotNull NamedBoundingBox named(@NotNull BoneName name) {
        return new NamedBoundingBox(name, this);
    }

    /**
     * Returns the width of the bounding box (X-axis extent).
     *
     * @return the width
     * @since 1.15.2
     */
    public double x() {
        return maxX - minX;
    }

    /**
     * Returns the height of the bounding box (Y-axis extent).
     *
     * @return the height
     * @since 1.15.2
     */
    public double y() {
        return maxY - minY;
    }

    /**
     * Returns the Y coordinate of the center of the bounding box.
     *
     * @return the center Y
     * @since 1.15.2
     */
    public double centerY() {
        return (maxY + minY) / 2;
    }

    /**
     * Returns the depth of the bounding box (Z-axis extent).
     *
     * @return the depth
     * @since 1.15.2
     */
    public double z() {
        return maxZ - minZ;
    }

    /**
     * Returns the center point of the bounding box as a vector.
     *
     * @return the center vector
     * @since 1.15.2
     */
    public @NotNull Vector3d centerPoint() {
        return new Vector3d(
            minX + maxX,
            minY + maxY,
            minZ + maxZ
        ).div(2D);
    }

    /**
     * Scales the bounding box by a uniform factor.
     *
     * @param scale the scale factor
     * @return the scaled bounding box
     * @since 1.15.2
     */
    public @NotNull ModelBoundingBox times(double scale) {
        return of(
            minX * scale,
            minY * scale,
            minZ * scale,
            maxX * scale,
            maxY * scale,
            maxZ * scale
        );
    }

    /**
     * Returns a new bounding box with the same dimensions but centered at the origin (0,0,0).
     *
     * @return the centered bounding box
     * @since 1.15.2
     */
    public @NotNull ModelBoundingBox center() {
        var center = centerPoint();
        return of(
            minX - center.x,
            minY - center.y,
            minZ - center.z,
            maxX - center.x,
            maxY - center.y,
            maxZ - center.z
        );
    }

    /**
     * Inverts the X and Z coordinates of the bounding box.
     *
     * @return the inverted bounding box
     * @since 1.15.2
     */
    public @NotNull ModelBoundingBox invert() {
        return of(
            -minX,
            minY,
            -minZ,
            -maxX,
            maxY,
            -maxZ
        );
    }

    /**
     * Rotates the bounding box around its center.
     *
     * @param quaterniond the rotation quaternion
     * @return the rotated bounding box
     * @since 1.15.2
     */
    public @NotNull ModelBoundingBox rotate(@NotNull Quaterniond quaterniond) {
        var centerVec = centerPoint();
        return of(
            min().sub(centerVec).rotate(quaterniond).add(centerVec),
            max().sub(centerVec).rotate(quaterniond).add(centerVec)
        );
    }

    /**
     * Returns the minimum corner as a vector.
     *
     * @return the min vector
     * @since 1.15.2
     */
    public @NotNull Vector3d min() {
        return new Vector3d(minX, minY, minZ);
    }

    /**
     * Returns the maximum corner as a vector.
     *
     * @return the max vector
     * @since 1.15.2
     */
    public @NotNull Vector3d max() {
        return new Vector3d(maxX, maxY, maxZ);
    }

    /**
     * Calculates the diagonal length in the XZ plane.
     *
     * @return the XZ diagonal length
     * @since 1.15.2
     */
    public double lengthZX() {
        return Math.sqrt(Math.pow(x(), 2) + Math.pow(z(), 2));
    }

    /**
     * Calculates the full diagonal length of the bounding box.
     *
     * @return the diagonal length
     * @since 1.15.2
     */
    public double length() {
        return Math.sqrt(Math.pow(x(), 2) + Math.pow(y(), 2) + Math.pow(z(), 2));
    }
}
