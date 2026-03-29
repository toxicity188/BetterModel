/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.util;

import it.unimi.dsi.fastutil.floats.FloatComparator;
import it.unimi.dsi.fastutil.floats.FloatSet;
import kr.toxicity.model.api.data.Float3;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

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
     * Minecraft tick mills
     */
    public static final int MINECRAFT_TICK_MILLS = 50;

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
     * Degrees to packed byte
     */
    public static final float DEGREES_TO_PACKED_BYTE = 256F / 360F;

    /**
     * Multiplier value for convert model size to block size
     */
    public static final float MODEL_TO_BLOCK_MULTIPLIER = 16;

    /**
     * Frame epsilon value
     */
    public static final float FRAME_EPSILON = 0.001F;

    /**
     * Float comparison epsilon value
     */
    public static final float FLOAT_COMPARISON_EPSILON = 1E-5F;

    /**
     * Squared vector comparison epsilon value
     */
    public static final float VECTOR_COMPARISON_EPSILON_SQ = 1E-8F;

    /**
     * Quaternion comparison epsilon value
     */
    public static final float QUATERNION_COMPARISON_EPSILON = 1E-5F;

    private static final Vector3f ZERO_VECTOR = new Vector3f();

    /**
     * Float comparator
     */
    public static final FloatComparator FRAME_COMPARATOR = (a, b) -> isSimilar(a, b, FRAME_EPSILON) ? 0 : Float.compare(a, b);

    private static final FloatSet VALID_ROTATION_DEGREES = FloatSet.of(
        0F,
        ROTATION_DEGREE,
        ROTATION_DEGREE * 2,
        -ROTATION_DEGREE,
        -ROTATION_DEGREE * 2
    );

    /**
     * Checks two floats are similar.
     * @param a a
     * @param b b
     * @return similar or not
     */
    public static boolean isSimilar(float a, float b) {
        return isSimilar(a, b, FLOAT_COMPARISON_EPSILON);
    }

    /**
     * Checks two floats are similar.
     * @param a a
     * @param b b
     * @param epsilon epsilon
     * @return similar or not
     */
    public static boolean isSimilar(float a, float b, float epsilon) {
        return abs(a - b) < epsilon;
    }

    /**
     * Checks two vectors are similar.
     * @param a a
     * @param b b
     * @return similar or not
     */
    public static boolean isSimilar(@NotNull Vector3fc a, @NotNull Vector3fc b) {
        return isSimilar(a, b, VECTOR_COMPARISON_EPSILON_SQ);
    }

    /**
     * Checks two vectors are similar.
     * @param a a
     * @param b b
     * @param epsilon epsilon
     * @return similar or not
     */
    public static boolean isSimilar(@NotNull Vector3fc a, @NotNull Vector3fc b, float epsilon) {
        return a.distanceSquared(b) < epsilon;
    }

    /**
     * Checks two quaternion are similar.
     * @param a a
     * @param b b
     * @return similar or not
     */
    public static boolean isSimilar(@NotNull Quaternionf a, @NotNull Quaternionf b) {
        return isSimilar(a, b, QUATERNION_COMPARISON_EPSILON);
    }

    /**
     * Checks two quaternion are similar.
     * @param a a
     * @param b b
     * @param epsilon epsilon
     * @return similar or not
     */
    public static boolean isSimilar(@NotNull Quaternionf a, @NotNull Quaternionf b, float epsilon) {
        return abs(fma(a.x, b.x, fma(a.y, b.y, fma(a.z, b.z, a.w * b.w)))) > 1.0F - epsilon;
    }

    /**
     * Creates epsilon-based hashcode of given float
     * @param value value
     * @return hashcode
     */
    public static int similarHashCode(float value) {
        return (int) (value / FLOAT_COMPARISON_EPSILON);
    }

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
     * Converts vector rotation to quaternion
     * @param vector vector
     * @return rotation
     */
    public static @NotNull Quaternionf toQuaternion(@NotNull Vector3f vector) {
        return toQuaternion(vector, new Quaternionf());
    }

    /**
     * Converts vector rotation to quaternion
     * @param vector vector
     * @param dest destination quaternion
     * @return rotation
     */
    public static @NotNull Quaternionf toQuaternion(@NotNull Vector3f vector, @NotNull Quaternionf dest) {
        return dest
            .identity()
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
        return toQuaternion(vec)
            .getEulerAnglesXYZ(vec)
            .mul(RADIANS_TO_DEGREES);
    }

    /**
     * Executes fused multiply add (a * b + c)
     * @param a a vector
     * @param b b vector
     * @param c c vector
     * @return added a
     */
    public static @NotNull Vector3f fma(@NotNull Vector3f a, @NotNull Vector3f b, @NotNull Vector3f c) {
        a.x = fma(a.x, b.x, c.x);
        a.y = fma(a.y, b.y, c.y);
        a.z = fma(a.z, b.z, c.z);
        return a;
    }

    /**
     * Executes fused multiply add (a * b + c)
     * @param a a vector
     * @param b b scala
     * @param c c vector
     * @return added a
     */
    public static @NotNull Vector3f fma(@NotNull Vector3f a, float b, @NotNull Vector3f c) {
        a.x = fma(a.x, b, c.x);
        a.y = fma(a.y, b, c.y);
        a.z = fma(a.z, b, c.z);
        return a;
    }

    /**
     * Executes fused multiply add (a * b + c)
     * @param a a
     * @param b b
     * @param c c
     * @return a * b + c
     */
    public static float fma(float a, float b, float c) {
        return org.joml.Math.fma(a, b, c);
    }

    /**
     * Executes fused multiply add (a * b + c)
     * @param a a
     * @param b b
     * @param c c
     * @return a * b + c
     */
    public static double fma(double a, double b, double c) {
        return org.joml.Math.fma(a, b, c);
    }

    /**
     * Checks this vector is not zero
     * @param vector3f vector
     * @return is not zero
     */
    public static boolean isNotZero(@NotNull Vector3f vector3f) {
        return !isZero(vector3f);
    }

    /**
     * Checks this vector is zero
     * @param vector vector
     * @return is zero
     */
    public static boolean isZero(@NotNull Vector3f vector) {
        return isSimilar(vector, ZERO_VECTOR);
    }
}
