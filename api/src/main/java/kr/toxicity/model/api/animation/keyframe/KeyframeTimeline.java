/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.animation.keyframe;

import kr.toxicity.model.api.util.MathUtil;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public sealed interface KeyframeTimeline {

    @NotNull Vector3f apply(int index, @NotNull KeyframeRuntimeContext context, @NotNull Vector3f dest, @NotNull KeyframeOperator operator);

    boolean isZero(int index);

    static @NotNull Builder constantBuilder(int size) {
        return new Constant.Bd(new float[size * 3]);
    }

    static @NotNull Builder runtimeBuilder(int size) {
        return new Runtime.Bd(new KeyframeVector[size]);
    }

    sealed interface Builder {
        void append(@NotNull KeyframeVector vector);

        @NotNull KeyframeTimeline build();
    }

    record Constant(float[] value) implements KeyframeTimeline {

        @Override
        public @NotNull Vector3f apply(int index, @NotNull KeyframeRuntimeContext context, @NotNull Vector3f dest, @NotNull KeyframeOperator operator) {
            var i = index * 3;
            return operator.operate(value[i], value[i + 1], value[i + 2], dest);
        }

        @Override
        public boolean isZero(int index) {
            var i = index * 3;
            return MathUtil.isSimilar(value[i], 0F)
                && MathUtil.isSimilar(value[i + 1], 0F)
                && MathUtil.isSimilar(value[i + 2], 0F);
        }

        @RequiredArgsConstructor
        private static final class Bd implements Builder {

            private final float[] target;
            private final Vector3f cache = new Vector3f();
            private int index;

            @Override
            public void append(@NotNull KeyframeVector vector) {
                vector.eval(KeyframeRuntimeContext.empty(), cache, KeyframeOperator.SET);
                target[index++] = cache.x;
                target[index++] = cache.y;
                target[index++] = cache.z;
            }

            @Override
            public @NotNull KeyframeTimeline build() {
                return new Constant(target);
            }
        }
    }

    record Runtime(@NotNull KeyframeVector[] value) implements KeyframeTimeline {
        @Override
        public @NotNull Vector3f apply(int index, @NotNull KeyframeRuntimeContext context, @NotNull Vector3f dest, @NotNull KeyframeOperator operator) {
            return value[index].eval(context, dest, operator);
        }

        @Override
        public boolean isZero(int index) {
            return value[index].isZero();
        }

        @RequiredArgsConstructor
        private static final class Bd implements Builder {

            private final KeyframeVector[] target;
            private int index;

            @Override
            public void append(@NotNull KeyframeVector vector) {
                target[index++] = vector;
            }

            @Override
            public @NotNull KeyframeTimeline build() {
                return new Runtime(target);
            }
        }
    }
}
