package kr.toxicity.model.api.util;

import it.unimi.dsi.fastutil.floats.FloatSet;
import kr.toxicity.model.api.data.raw.Float3;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static java.lang.Math.*;

/**
 * Math
 */
@ApiStatus.Internal
public final class MathUtil {

    /**
     * No initializer
     */
    private MathUtil() {
        throw new RuntimeException();
    }

    /**
     * Valid rotation degree
     */
    public static final float ROTATION_DEGREE = 22.5F;

    /**
     * Degrees to radians
     */
    public static final float DEGREES_TO_RADIANS = (float) PI / 180F;

    /**
     * Radians to degrees
     */
    public static final float RADIANS_TO_DEGREES = 1F / DEGREES_TO_RADIANS;

    /**
     * Valid rotation degrees
     */
    public static final FloatSet VALID_ROTATION_DEGREES = FloatSet.of(
            0F,
            ROTATION_DEGREE,
            ROTATION_DEGREE * 2,
            -ROTATION_DEGREE,
            -ROTATION_DEGREE * 2
    );

    /**
     * Checks these floats are valid Minecraft degree
     * @param rotation rotation
     * @return is valid
     */
    public static boolean checkValidDegree(@NotNull Float3 rotation) {
        var i = 0;
        if (rotation.x() != 0F) i++;
        if (rotation.y() != 0F) i++;
        if (rotation.z() != 0F) i++;
        return i < 2 && checkValidDegree(rotation.x()) && checkValidDegree(rotation.y()) && checkValidDegree(rotation.z());
    }

    /**
     * Checks this float is valid Minecraft degree
     * @param rotation rotation
     * @return is valid
     */
    public static boolean checkValidDegree(float rotation) {
        return VALID_ROTATION_DEGREES.contains(rotation);
    }

    /**
     * Creates rotation identifier
     * @param rotation rotation
     * @return identifier
     */
    public static @NotNull Float3 identifier(@NotNull Float3 rotation) {
        if (checkValidDegree(rotation)) return Float3.ZERO;
        return rotation;
    }

    /**
     * Converts animation rotation to display rotation
     * @param vector original
     * @return vector
     */
    public static @NotNull Vector3f animationToDisplay(@NotNull Vector3f vector) {
        return new Vector3f(
                vector.x,
                -vector.y,
                -vector.z
        );
    }

    /**
     * Converts animation position to display position
     * @param vector original
     * @return vector
     */
    public static @NotNull Vector3f transformToDisplay(@NotNull Vector3f vector) {
        return new Vector3f(
                vector.x,
                vector.y,
                -vector.z
        );
    }

    /**
     * Converts vector rotation to quaternion
     * @param vector vector
     * @return rotation
     */
    public static @NotNull Quaternionf toQuaternion(@NotNull Vector3f vector) {
        return new Quaternionf()
                .rotateZYX(
                        vector.z * DEGREES_TO_RADIANS,
                        vector.y * DEGREES_TO_RADIANS,
                        vector.x * DEGREES_TO_RADIANS
                );
    }

    /**
     * Converts zyx euler to xyz euler
     * @param vec zyx euler
     * @return xyz euler
     */
    public static @NotNull Vector3f toXYZEuler(@NotNull Vector3f vec) {
        return toXYZEuler(toQuaternion(vec));
    }

    /**
     * Gets rotation matrix of quaternion
     * @param quaternion rotation
     * @return matrix
     */
    public static @NotNull Matrix3f toMatrix(@NotNull Quaternionf quaternion) {
        return quaternion.get(new Matrix3f());
    }

    /**
     * Converts zxy euler to xyz euler
     * @param quaternion rotation
     * @return xyz euler
     */
    public static @NotNull Vector3f toXYZEuler(@NotNull Quaternionf quaternion) {
        return toXYZEuler(toMatrix(quaternion));
    }


    /**
     * Gets xyz euler of this matrix
     * @param mat matrix
     * @return xyz euler
     */
    public static @NotNull Vector3f toXYZEuler(@NotNull Matrix3f mat) {
        var ret = new Vector3f();
        if (abs(mat.m20) < 1F) {
            ret.x = (float) atan2(-mat.m21, mat.m22);
            ret.z = (float) atan2(-mat.m10, mat.m00);
        } else {
            ret.x = (float) atan2(mat.m12, mat.m11);
            ret.z = 0F;
        }
        ret.y = (float) asin(clamp(mat.m20, -1F, 1F));
        return ret.mul(RADIANS_TO_DEGREES);
    }
}
