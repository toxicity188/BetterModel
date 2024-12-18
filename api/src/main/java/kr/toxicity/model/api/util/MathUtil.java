package kr.toxicity.model.api.util;

import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static java.lang.Math.*;

public final class MathUtil {
    private MathUtil() {
        throw new RuntimeException();
    }

    public static @NotNull Vector3f animationToDisplay(@NotNull Vector3f vector) {
        return new Vector3f(
                -vector.x,
                -vector.y,
                vector.z
        );
    }
    public static @NotNull Vector3f blockBenchToDisplay(@NotNull Vector3f vector) {
        return new Vector3f(
                -vector.x,
                vector.y,
                -vector.z
        );
    }

    public static @NotNull Quaternionf toQuaternion(@NotNull Vector3f vector) {
        var rotate = new Vector3f(vector).div(180).mul((float) PI);
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
}
