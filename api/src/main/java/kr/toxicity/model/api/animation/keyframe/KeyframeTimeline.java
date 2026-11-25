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

/**
 * Stores sampled animation vectors by zero-based index. A {@link Constant} timeline stores
 * pre-evaluated values. A {@link Runtime} timeline evaluates its samples with the supplied runtime context.
 * <pre>{@code
 * var builder = KeyframeTimeline.constantBuilder(1);
 * builder.append(KeyframeVector.ZERO);
 * var timeline = builder.build();
 * }</pre>
 *
 * @since 3.7.0
 */
public sealed interface KeyframeTimeline {

    /**
     * Applies the selected sample to the destination using the supplied operation.
     * <pre>{@code
     * timeline.apply(0, KeyframeRuntimeContext.empty(), new Vector3f(), KeyframeOperator.SET);
     * }</pre>
     *
     * @param index the zero-based sample index
     * @param context the runtime context used by runtime samples
     * @param dest the destination vector passed to the operation
     * @param operator the operation that combines the sample with the destination
     * @return the vector returned by the operation
     * @throws IndexOutOfBoundsException if the sample index is outside this timeline
     * @throws RuntimeException if runtime evaluation or the operation throws
     * @since 3.7.0
     */
    @NotNull Vector3f apply(int index, @NotNull KeyframeRuntimeContext context, @NotNull Vector3f dest, @NotNull KeyframeOperator operator);

    /**
     * Checks whether the stored sample is zero. Constant samples use approximate component comparison.
     * Runtime samples check for the shared zero vector without evaluating it.
     * <pre>{@code
     * boolean zero = timeline.isZero(0);
     * }</pre>
     *
     * @param index the zero-based sample index
     * @return true if the sample is zero according to its storage type
     * @throws IndexOutOfBoundsException if the sample index is outside this timeline
     * @since 3.7.0
     */
    boolean isZero(int index);

    /**
     * Creates a fixed-capacity builder for constant samples. Each sample is evaluated with an empty context when appended.
     * <pre>{@code
     * var builder = KeyframeTimeline.constantBuilder(1);
     * builder.append(KeyframeVector.ZERO);
     * }</pre>
     *
     * @param size the number of samples. Use a value from zero through {@code Integer.MAX_VALUE / 3}
     * @return the constant timeline builder
     * @throws NegativeArraySizeException if the computed component array length is negative
     * @since 3.7.0
     */
    static @NotNull Builder constantBuilder(int size) {
        return new Constant.Bd(new float[size * 3]);
    }

    /**
     * Creates a fixed-capacity builder that retains each vector for runtime evaluation.
     * <pre>{@code
     * var builder = KeyframeTimeline.runtimeBuilder(1);
     * builder.append(vector);
     * }</pre>
     *
     * @param size the number of samples
     * @return the runtime timeline builder
     * @throws NegativeArraySizeException if size is negative
     * @since 3.7.0
     */
    static @NotNull Builder runtimeBuilder(int size) {
        return new Runtime.Bd(new KeyframeVector[size]);
    }

    /**
     * Collects samples in index order for a timeline of a fixed size. Fill every slot before building.
     * <pre>{@code
     * var builder = KeyframeTimeline.constantBuilder(1);
     * builder.append(KeyframeVector.ZERO);
     * var timeline = builder.build();
     * }</pre>
     *
     * @since 3.7.0
     */
    sealed interface Builder {
        /**
         * Writes the next sample. A constant builder evaluates the vector immediately with an empty context.
         * <pre>{@code
         * builder.append(KeyframeVector.ZERO);
         * }</pre>
         *
         * @param vector the sample to append
         * @throws IndexOutOfBoundsException if the builder is full
         * @throws RuntimeException if evaluation throws while appending to a constant builder
         * @since 3.7.0
         */
        void append(@NotNull KeyframeVector vector);

        /**
         * Returns a timeline backed by the builder's sample storage. Call after appending all samples.
         * <pre>{@code
         * var timeline = builder.build();
         * }</pre>
         *
         * @return the timeline backed by the stored samples
         * @since 3.7.0
         */
        @NotNull KeyframeTimeline build();
    }

    /**
     * A timeline of pre-evaluated samples stored as consecutive X, Y, and Z components.
     * <pre>{@code
     * var timeline = new KeyframeTimeline.Constant(new float[] {1F, 2F, 3F});
     * }</pre>
     *
     * @param value the backing array of component triples, retained without copying
     * @since 3.7.0
     */
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

    /**
     * A timeline that retains compiled vectors for evaluation at runtime.
     * <pre>{@code
     * var timeline = new KeyframeTimeline.Runtime(new KeyframeVector[] {KeyframeVector.ZERO});
     * }</pre>
     *
     * @param value the backing array of samples in index order, retained without copying
     * @since 3.7.0
     */
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
