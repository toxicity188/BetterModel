/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.animation.keyframe;

import kr.toxicity.model.api.util.MathUtil;
import kr.toxicity.model.api.util.function.Float2FloatFunction;
import org.jetbrains.annotations.NotNull;

/**
 * A compiled scalar keyframe value that can be evaluated with a runtime context.
 * <pre>{@code
 * float value = Keyframe.of(2F).eval(KeyframeRuntimeContext.empty());
 * }</pre>
 *
 * @since 3.7.0
 */
@FunctionalInterface
public interface Keyframe {

    /**
     * The shared constant zero keyframe.
     * @since 3.7.0
     */
    Keyframe ZERO = new Constant(0F);

    /**
     * Evaluates this keyframe with the supplied runtime context.
     * <pre>{@code
     * float value = Keyframe.of(2F).eval(KeyframeRuntimeContext.empty());
     * }</pre>
     *
     * @param context the runtime query context
     * @return the evaluated scalar value
     * @throws RuntimeException if runtime evaluation fails
     * @since 3.7.0
     */
    float eval(@NotNull KeyframeRuntimeContext context);

    /**
     * Reports whether evaluation is independent of the runtime context.
     * <pre>{@code
     * boolean constant = Keyframe.of(2F).isConstant();
     * }</pre>
     *
     * @return true if this keyframe is known to be constant
     * @since 3.7.0
     */
    default boolean isConstant() {
        return false;
    }

    /**
     * Checks whether this keyframe is the shared {@link #ZERO} instance.
     * <pre>{@code
     * boolean zero = Keyframe.of(0F).isZero();
     * }</pre>
     *
     * @return true if this keyframe is the shared zero
     * @since 3.7.0
     */
    default boolean isZero() {
        return this == ZERO;
    }

    /**
     * Transforms the scalar result. Mapping a {@link Constant} applies the function immediately.
     * <pre>{@code
     * var doubled = Keyframe.of(2F).map(value -> value * 2F);
     * }</pre>
     *
     * @param function the transformation to apply
     * @return the mapped keyframe, or this keyframe for the identity sentinel
     * @throws RuntimeException if mapping a constant and the transformation throws
     * @since 3.7.0
     */
    default @NotNull Keyframe map(@NotNull Float2FloatFunction function) {
        return function == Float2FloatFunction.NONE ? this : ctx -> function.applyAsFloat(eval(ctx));
    }

    /**
     * Creates a constant scalar keyframe.
     * <pre>{@code
     * var keyframe = Keyframe.of(2F);
     * }</pre>
     *
     * @param value the scalar value
     * @return the shared zero if the value is approximately zero, or a constant keyframe
     * @since 3.7.0
     */
    static @NotNull Keyframe of(float value) {
        return MathUtil.isSimilar(value, 0F) ? ZERO : new Constant(value);
    }

    /**
     * Combines two scalar keyframes. If all operands are {@link Constant} values, applies the operator immediately.
     * <pre>{@code
     * var sum = Keyframe.operate(Keyframe.of(1F), Keyframe.of(2F),
     *     (a, b) -> a + b);
     * }</pre>
     *
     * @param p0 the first operand
     * @param p1 the second operand
     * @param operator the operation to apply to the operand values
     * @return the combined keyframe
     * @throws RuntimeException if all operands are constants and the operator throws
     * @since 3.7.0
     */
    static @NotNull Keyframe operate(@NotNull Keyframe p0, @NotNull Keyframe p1, @NotNull Keyframe.Operator2 operator) {
        return p0 instanceof Constant(float c0)
            && p1 instanceof Constant(float c1)
            ? of(operator.operate(c0, c1))
            : ctx -> operator.operate(p0.eval(ctx), p1.eval(ctx));
    }

    /**
     * Combines four scalar keyframes. If all operands are {@link Constant} values, applies the operator immediately.
     * <pre>{@code
     * var sum = Keyframe.operate(Keyframe.of(1F), Keyframe.of(2F), Keyframe.of(3F), Keyframe.of(4F),
     *     (a, b, c, d) -> a + b + c + d);
     * }</pre>
     *
     * @param p0 the first operand
     * @param p1 the second operand
     * @param p2 the third operand
     * @param p3 the fourth operand
     * @param operator the operation to apply to the operand values
     * @return the combined keyframe
     * @throws RuntimeException if all operands are constants and the operator throws
     * @since 3.7.0
     */
    static @NotNull Keyframe operate(@NotNull Keyframe p0, @NotNull Keyframe p1, @NotNull Keyframe p2, @NotNull Keyframe p3, @NotNull Keyframe.Operator4 operator) {
        return p0 instanceof Constant(float c0)
            && p1 instanceof Constant(float c1)
            && p2 instanceof Constant(float c2)
            && p3 instanceof Constant(float c3)
            ? of(operator.operate(c0, c1, c2, c3))
            : ctx -> operator.operate(p0.eval(ctx), p1.eval(ctx), p2.eval(ctx), p3.eval(ctx));
    }

    /**
     * A scalar keyframe that returns the same value for every runtime context.
     * <pre>{@code
     * var keyframe = Keyframe.of(2F);
     * }</pre>
     *
     * @param value the constant scalar value
     * @since 3.7.0
     */
    record Constant(float value) implements Keyframe {
        @Override
        public float eval(@NotNull KeyframeRuntimeContext context) {
            return value;
        }

        @Override
        public boolean isConstant() {
            return true;
        }

        @Override
        public @NotNull Keyframe map(@NotNull Float2FloatFunction function) {
            return function == Float2FloatFunction.NONE ? this : of(function.applyAsFloat(value));
        }
    }

    /**
     * An operation on two evaluated scalar keyframe values.
     * <pre>{@code
     * Keyframe.Operator2 sum = (a, b) -> a + b;
     * }</pre>
     *
     * @since 3.7.0
     */
    interface Operator2 {
        /**
         * Applies this operation to the supplied values.
         * <pre>{@code
         * Keyframe.Operator2 sum = (a, b) -> a + b;
         * float result = sum.operate(1F, 2F);
         * }</pre>
         *
         * @param p0 the first operand
         * @param p1 the second operand
         * @return the operation result
         * @since 3.7.0
         */
        float operate(float p0, float p1);
    }

    /**
     * An operation on four evaluated scalar keyframe values.
     * <pre>{@code
     * Keyframe.Operator4 sum = (a, b, c, d) -> a + b + c + d;
     * }</pre>
     *
     * @since 3.7.0
     */
    interface Operator4 {
        /**
         * Applies this operation to the supplied values.
         * <pre>{@code
         * Keyframe.Operator4 sum = (a, b, c, d) -> a + b + c + d;
         * float result = sum.operate(1F, 2F, 3F, 4F);
         * }</pre>
         *
         * @param p0 the first operand
         * @param p1 the second operand
         * @param p2 the third operand
         * @param p3 the fourth operand
         * @return the operation result
         * @since 3.7.0
         */
        float operate(float p0, float p1, float p2, float p3);
    }
}
