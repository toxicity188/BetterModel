package kr.toxicity.model.api.data.blueprint;

import kr.toxicity.model.api.bone.BoneName;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

/**
 * Model bounding box
 * @param minX min-x
 * @param minY min-y
 * @param minZ min-z
 * @param maxX max-x
 * @param maxY max-y
 * @param maxZ max-z
 */
public record ModelBoundingBox(
        double minX,
        double minY,
        double minZ,
        double maxX,
        double maxY,
        double maxZ
) {

    public static final ModelBoundingBox MIN = of(0.1, 0.1, 0.1);

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
     * Gets bounding box with name
     * @param name name
     * @return named box
     */
    public @NotNull NamedBoundingBox named(@NotNull BoneName name) {
        return new NamedBoundingBox(name, this);
    }

    /**
     * Gets x
     * @return x
     */
    public double x() {
        return maxX - minX;
    }

    /**
     * Gets y
     * @return y
     */
    public double y() {
        return maxY - minY;
    }

    /**
     * Gets center y
     * @return center y
     */
    public double centerY() {
        return (maxY + minY) / 2;
    }

    /**
     * Gets z
     * @return z
     */
    public double z() {
        return maxZ - minZ;
    }

    /**
     * Gets center vector point
     * @return center
     */
    public @NotNull Vector3d centerPoint() {
        return new Vector3d(
                minX + maxX,
                minY + maxY,
                minZ + maxZ
        ).div(2D);
    }

    /**
     * Gets scaled bounding box
     * @param scale scale
     * @return scaled bounding box
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
     * Gets centered bounding box
     * @return centered bounding box
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

    public @NotNull ModelBoundingBox rotate(@NotNull Quaterniond quaterniond) {
        var centerVec = centerPoint();
        return of(
                min().sub(centerVec).rotate(quaterniond).add(centerVec),
                max().sub(centerVec).rotate(quaterniond).add(centerVec)
        );
    }

    public @NotNull Vector3d min() {
        return new Vector3d(minX, minY, minZ);
    }

    public @NotNull Vector3d max() {
        return new Vector3d(maxX, maxY, maxZ);
    }

    /**
     * Gets zx length of bounding box
     * @return zx length
     */
    public double lengthZX() {
        return Math.sqrt(Math.pow(x(), 2) + Math.pow(z(), 2));
    }

    /**
     * Gets length of bounding box
     * @return length
     */
    public double length() {
        return Math.sqrt(Math.pow(x(), 2) + Math.pow(y(), 2) + Math.pow(z(), 2));
    }
}
