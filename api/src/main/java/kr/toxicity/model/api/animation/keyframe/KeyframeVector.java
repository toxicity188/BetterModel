/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.animation.keyframe;

import kr.toxicity.model.api.util.function.Float2FloatFunction;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

/**
 * A compiled vector with a scalar {@link Keyframe} for each component.
 * <pre>{@code
 * var vector = KeyframeVector.of(Keyframe.of(2F), Keyframe.ZERO, Keyframe.ZERO);
 * var position = vector.eval();
 * }</pre>
 *
 * @param x the X keyframe
 * @param y the Y keyframe
 * @param z the Z keyframe
 * @since 3.7.0
 */
public record KeyframeVector(
    @NotNull Keyframe x,
    @NotNull Keyframe y,
    @NotNull Keyframe z
) {

    /**
     * The shared vector of three zero keyframes.
     * @since 3.7.0
     */
    public static final KeyframeVector ZERO = new KeyframeVector(
        Keyframe.ZERO,
        Keyframe.ZERO,
        Keyframe.ZERO
    );

    /**
     * Creates a compiled vector from its scalar components.
     * <pre>{@code
     * var vector = KeyframeVector.of(Keyframe.of(2F), Keyframe.ZERO, Keyframe.ZERO);
     * }</pre>
     *
     * @param x the X keyframe
     * @param y the Y keyframe
     * @param z the Z keyframe
     * @return the shared zero vector if all components report zero, or a new vector
     * @since 3.7.0
     */
    public static @NotNull KeyframeVector of(@NotNull Keyframe x, @NotNull Keyframe y, @NotNull Keyframe z) {
        return x.isZero() && y.isZero() && z.isZero() ? ZERO : new KeyframeVector(x, y, z);
    }

    /**
     * Checks whether this vector is the shared {@link #ZERO} instance.
     * <pre>{@code
     * boolean zero = KeyframeVector.ZERO.isZero();
     * }</pre>
     *
     * @return true if this vector is the shared zero
     * @since 3.7.0
     */
    public boolean isZero() {
        return this == ZERO;
    }

    /**
     * Checks whether all components are constant.
     * <pre>{@code
     * boolean constant = KeyframeVector.ZERO.isConstant();
     * }</pre>
     *
     * @return true if each scalar keyframe reports that it is constant
     * @since 3.7.0
     */
    public boolean isConstant() {
        return x.isConstant() && y.isConstant() && z.isConstant();
    }

    /**
     * Combines two vectors by applying the same binary operation to each pair of components.
     * <pre>{@code
     * var sum = KeyframeVector.operate(vector, KeyframeVector.ZERO, (a, b) -> a + b);
     * }</pre>
     *
     * @param p0 the first vector
     * @param p1 the second vector
     * @param operator the operation applied to all three component pairs
     * @return the combined vector
     * @throws RuntimeException if the operator throws during constant folding
     * @since 3.7.0
     */
    public static @NotNull KeyframeVector operate(
        @NotNull KeyframeVector p0,
        @NotNull KeyframeVector p1,
        @NotNull Keyframe.Operator2 operator
    ) {
        return operate(p0, p1, operator, operator, operator);
    }

    /**
     * Combines two vectors using a separate binary operation for each axis.
     * <pre>{@code
     * var combined = KeyframeVector.operate(vector, other,
     *     (a, b) -> a + b, (a, b) -> a - b, (a, b) -> a * b);
     * }</pre>
     *
     * @param p0 the first vector
     * @param p1 the second vector
     * @param xOperator the operation for the X components
     * @param yOperator the operation for the Y components
     * @param zOperator the operation for the Z components
     * @return the combined vector
     * @throws RuntimeException if a component operator throws during constant folding
     * @since 3.7.0
     */
    public static @NotNull KeyframeVector operate(
        @NotNull KeyframeVector p0,
        @NotNull KeyframeVector p1,
        @NotNull Keyframe.Operator2 xOperator,
        @NotNull Keyframe.Operator2 yOperator,
        @NotNull Keyframe.Operator2 zOperator
    ) {
        return of(
            Keyframe.operate(p0.x, p1.x, xOperator),
            Keyframe.operate(p0.y, p1.y, yOperator),
            Keyframe.operate(p0.z, p1.z, zOperator)
        );
    }

    /**
     * Combines four vectors by applying the same operation to the four values on each axis.
     * <pre>{@code
     * var sum = KeyframeVector.operate(first, second, third, fourth,
     *     (a, b, c, d) -> a + b + c + d);
     * }</pre>
     *
     * @param p0 the first vector
     * @param p1 the second vector
     * @param p2 the third vector
     * @param p3 the fourth vector
     * @param operator the operation applied to the four components on each axis
     * @return the combined vector
     * @throws RuntimeException if the operator throws during constant folding
     * @since 3.7.0
     */
    public static @NotNull KeyframeVector operate(@NotNull KeyframeVector p0, @NotNull KeyframeVector p1, @NotNull KeyframeVector p2, @NotNull KeyframeVector p3, @NotNull Keyframe.Operator4 operator) {
        return of(
            Keyframe.operate(p0.x, p1.x, p2.x, p3.x, operator),
            Keyframe.operate(p0.y, p1.y, p2.y, p3.y, operator),
            Keyframe.operate(p0.z, p1.z, p2.z, p3.z, operator)
        );
    }

    /**
     * Applies the same scalar transformation to each component.
     * <pre>{@code
     * var doubled = vector.map(value -> value * 2F);
     * }</pre>
     *
     * @param function the transformation for each component
     * @return the mapped vector, or this vector for the identity sentinel
     * @throws RuntimeException if the transformation throws while mapping a constant component
     * @since 3.7.0
     */
    public @NotNull KeyframeVector map(@NotNull Float2FloatFunction function) {
        return function == Float2FloatFunction.NONE ? this : of(
            x.map(function),
            y.map(function),
            z.map(function)
        );
    }

    /**
     * Evaluates this vector with the shared empty runtime context.
     * <pre>{@code
     * var position = KeyframeVector.ZERO.eval();
     * }</pre>
     *
     * @return a new vector containing the evaluated components
     * @throws RuntimeException if evaluating a component throws
     * @since 3.7.0
     */
    public @NotNull Vector3f eval() {
        return eval(KeyframeRuntimeContext.empty());
    }

    /**
     * Evaluates this vector with the supplied runtime context.
     * <pre>{@code
     * var position = vector.eval(context);
     * }</pre>
     *
     * @param context the runtime context passed to each component
     * @return a new vector containing the evaluated components
     * @throws RuntimeException if evaluating a component throws
     * @since 3.7.0
     */
    public @NotNull Vector3f eval(@NotNull KeyframeRuntimeContext context) {
        return eval(context, new Vector3f(), KeyframeOperator.SET);
    }

    /**
     * Evaluates each component and passes the results to a destination operation.
     * <pre>{@code
     * vector.eval(context, position, KeyframeOperator.ADD);
     * }</pre>
     *
     * @param context the runtime context passed to each component
     * @param dest the destination vector passed to the operation
     * @param operator the operation that combines the evaluated values with the destination
     * @return the vector returned by the operation
     * @throws RuntimeException if a component evaluation or the operation throws
     * @since 3.7.0
     */
    public @NotNull Vector3f eval(@NotNull KeyframeRuntimeContext context, @NotNull Vector3f dest, @NotNull KeyframeOperator operator) {
        return operator.operate(x.eval(context), y.eval(context), z.eval(context), dest);
    }

    /**
     * Transforms each component with the corresponding axis mapping.
     * <pre>{@code
     * var offset = vector.withAxis(KeyframeAxis.SCALE);
     * }</pre>
     *
     * @param axis the per-axis transformations
     * @return the transformed vector
     * @throws RuntimeException if an axis transformation throws while mapping a constant component
     * @since 3.7.0
     */
    public @NotNull KeyframeVector withAxis(@NotNull KeyframeAxis axis) {
        return of(
            x.map(axis.x()),
            y.map(axis.y()),
            z.map(axis.z())
        );
    }
}
