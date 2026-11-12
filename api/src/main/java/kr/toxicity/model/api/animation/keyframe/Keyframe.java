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

@FunctionalInterface
public interface Keyframe {

    Keyframe ZERO = new Constant(0F);

    float eval(@NotNull KeyframeRuntimeContext context);

    default boolean isConstant() {
        return false;
    }

    default boolean isZero() {
        return this == ZERO;
    }

    default @NotNull Keyframe map(@NotNull Float2FloatFunction function) {
        return function == Float2FloatFunction.NONE ? this : ctx -> function.applyAsFloat(eval(ctx));
    }

    static @NotNull Keyframe of(float value) {
        return MathUtil.isSimilar(value, 0F) ? ZERO : new Constant(value);
    }

    static @NotNull Keyframe operate(@NotNull Keyframe p0, @NotNull Keyframe p1, @NotNull Keyframe.Operator2 operator) {
        return p0 instanceof Constant(float c0)
            && p1 instanceof Constant(float c1)
            ? of(operator.operate(c0, c1))
            : ctx -> operator.operate(p0.eval(ctx), p1.eval(ctx));
    }

    static @NotNull Keyframe operate(@NotNull Keyframe p0, @NotNull Keyframe p1, @NotNull Keyframe p2, @NotNull Keyframe p3, @NotNull Keyframe.Operator4 operator) {
        return p0 instanceof Constant(float c0)
            && p1 instanceof Constant(float c1)
            && p2 instanceof Constant(float c2)
            && p3 instanceof Constant(float c3)
            ? of(operator.operate(c0, c1, c2, c3))
            : ctx -> operator.operate(p0.eval(ctx), p1.eval(ctx), p2.eval(ctx), p3.eval(ctx));
    }

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

    interface Operator2 {
        float operate(float p0, float p1);
    }

    interface Operator4 {
        float operate(float p0, float p1, float p2, float p3);
    }
}
