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

public record KeyframeVector(
    @NotNull Keyframe x,
    @NotNull Keyframe y,
    @NotNull Keyframe z
) {

    public static final KeyframeVector ZERO = new KeyframeVector(
        Keyframe.ZERO,
        Keyframe.ZERO,
        Keyframe.ZERO
    );

    public static @NotNull KeyframeVector of(@NotNull Keyframe x, @NotNull Keyframe y, @NotNull Keyframe z) {
        return x.isZero() && y.isZero() && z.isZero() ? ZERO : new KeyframeVector(x, y, z);
    }

    public boolean isZero() {
        return this == ZERO;
    }

    public boolean isConstant() {
        return x.isConstant() && y.isConstant() && z.isConstant();
    }

    public static @NotNull KeyframeVector operate(
        @NotNull KeyframeVector p0,
        @NotNull KeyframeVector p1,
        @NotNull Keyframe.Operator2 operator
    ) {
        return operate(p0, p1, operator, operator, operator);
    }

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

    public static @NotNull KeyframeVector operate(@NotNull KeyframeVector p0, @NotNull KeyframeVector p1, @NotNull KeyframeVector p2, @NotNull KeyframeVector p3, @NotNull Keyframe.Operator4 operator) {
        return of(
            Keyframe.operate(p0.x, p1.x, p2.x, p3.x, operator),
            Keyframe.operate(p0.y, p1.y, p2.y, p3.y, operator),
            Keyframe.operate(p0.z, p1.z, p2.z, p3.z, operator)
        );
    }

    public @NotNull KeyframeVector map(@NotNull Float2FloatFunction function) {
        return function == Float2FloatFunction.NONE ? this : of(
            x.map(function),
            y.map(function),
            z.map(function)
        );
    }

    public @NotNull Vector3f eval() {
        return eval(KeyframeRuntimeContext.empty());
    }

    public @NotNull Vector3f eval(@NotNull KeyframeRuntimeContext context) {
        return eval(context, new Vector3f(), KeyframeOperator.SET);
    }

    public @NotNull Vector3f eval(@NotNull KeyframeRuntimeContext context, @NotNull Vector3f dest, @NotNull KeyframeOperator operator) {
        return operator.operate(x.eval(context), y.eval(context), z.eval(context), dest);
    }

    public @NotNull KeyframeVector withAxis(@NotNull KeyframeAxis axis) {
        return of(
            x.map(axis.x()),
            y.map(axis.y()),
            z.map(axis.z())
        );
    }
}
