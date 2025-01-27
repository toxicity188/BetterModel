package kr.toxicity.model.api.util;

import kr.toxicity.model.api.data.raw.Float3;
import org.jetbrains.annotations.NotNull;
import org.joml.*;

import java.lang.Math;
import java.util.Set;

import static java.lang.Math.*;

/**
 * Math
 */
public final class MathUtil {

    /**
     * No initializer
     */
    private MathUtil() {
        throw new RuntimeException();
    }

    public static final float ROTATION_DEGREE = 22.5F;

    public static final Set<Float> VALID_ROTATION_DEGREES = Set.of(
            0F,
            ROTATION_DEGREE,
            ROTATION_DEGREE * 2,
            -ROTATION_DEGREE,
            -ROTATION_DEGREE * 2
    );

    public static boolean checkValidDegree(@NotNull Float3 rotation) {
        var i = 0;
        if (rotation.x() != 0F) i++;
        if (rotation.y() != 0F) i++;
        if (rotation.z() != 0F) i++;
        return i < 2 && checkValidDegree(rotation.x()) && checkValidDegree(rotation.y()) && checkValidDegree(rotation.z());
    }

    public static boolean checkValidDegree(float rotation) {
        return VALID_ROTATION_DEGREES.contains(rotation);
    }

    public static @NotNull Float3 identifier(@NotNull Float3 rotation) {
        if (checkValidDegree(rotation)) return Float3.ZERO;
        return rotation;
    }

    private static float createIdentifier(float value) {
        var abs = Math.abs(value);
        if (abs >= ROTATION_DEGREE) abs -= ROTATION_DEGREE;
        if (abs >= ROTATION_DEGREE) abs -= ROTATION_DEGREE;
        return (value > 0 ? 1 : -1) * abs;
    }

    /**
     * Converts animation rotation to display rotation
     * @param vector original
     * @return vector
     */
    public static @NotNull Vector3f animationToDisplay(@NotNull Vector3f vector) {
        return new Vector3f(
                -vector.x,
                -vector.y,
                vector.z
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
     * Converts BlockBench position(right-hand) to display position(left-hand)
     * @param vector original
     * @return vector
     */
    public static @NotNull Vector3f blockBenchToDisplay(@NotNull Vector3f vector) {
        return new Vector3f(
                -vector.x,
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
        var rotate = toRadians(vector);
        var roll = rotate.x;
        var pitch = rotate.y;
        var yaw = rotate.z;

        var cr = cos(roll * 0.5);
        var sr = sin(roll * 0.5);
        var cp = cos(pitch * 0.5);
        var sp = sin(pitch * 0.5);
        var cy = cos(yaw * 0.5);
        var sy = sin(yaw * 0.5);

        var q = new Quaternionf();
        q.w = (float) (cr * cp * cy + sr * sp * sy);
        q.x = (float) (sr * cp * cy - cr * sp * sy);
        q.y = (float) (cr * sp * cy + sr * cp * sy);
        q.z = (float) (cr * cp * sy - sr * sp * cy);

        return q;
    }

    public static @NotNull Vector3f toRadians(@NotNull Vector3f vector) {
        return new Vector3f(vector).div(180).mul((float) PI);
    }
    public static @NotNull Vector3f toDegrees(@NotNull Vector3f vector) {
        return new Vector3f(vector).mul(180).div((float) PI);
    }

    public static @NotNull Vector3f toMinecraftVector(@NotNull Vector3f vec) {
        return toXYZEuler(toQuaternion(vec));
    }

    public static @NotNull Matrix3f toMatrix(@NotNull Quaternionf quaternion) {
        return quaternion.get(new Matrix3f());
    }

    public static @NotNull Vector3f toXYZEuler(@NotNull Quaternionf quaternion) {
        return toXYZEuler(toMatrix(quaternion));
    }

    public static Vector3f toXYZEuler(Matrix3f mat) {
        var ret = new Vector3f();
        ret.y = (float) asin(clamp(mat.m20, -1F, 1F));
        if (abs(mat.m20) < 1F) {
            ret.x = (float) atan2(-mat.m21, mat.m22);
            ret.z = (float) atan2(-mat.m10, mat.m00);
        } else {
            ret.x = (float) atan2(mat.m12, mat.m11);
            ret.z = 0F;
        }

        return toDegrees(ret);
    }
}
