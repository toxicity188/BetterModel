/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.animation.keyframe;

import org.jetbrains.annotations.NotNull;

/**
 * Three uncompiled scalar datapoints representing one animation vector.
 * <pre>{@code
 * var datapoints = KeyframeDatapointVector.of(
 *     KeyframeDatapoint.of(2F), KeyframeDatapoint.ZERO, KeyframeDatapoint.ZERO);
 * }</pre>
 *
 * @param x the X datapoint
 * @param y the Y datapoint
 * @param z the Z datapoint
 * @since 3.7.0
 */
public record KeyframeDatapointVector(
    @NotNull KeyframeDatapoint x,
    @NotNull KeyframeDatapoint y,
    @NotNull KeyframeDatapoint z
) {

    /**
     * The vector containing three numeric zero datapoints.
     * @since 3.7.0
     */
    public static final KeyframeDatapointVector ZERO = of(KeyframeDatapoint.ZERO, KeyframeDatapoint.ZERO, KeyframeDatapoint.ZERO);

    /**
     * Creates a vector from its component datapoints.
     * <pre>{@code
     * var datapoints = KeyframeDatapointVector.of(
     *     KeyframeDatapoint.of(2F), KeyframeDatapoint.ZERO, KeyframeDatapoint.ZERO);
     * }</pre>
     *
     * @param x the X datapoint
     * @param y the Y datapoint
     * @param z the Z datapoint
     * @return the datapoint vector
     * @since 3.7.0
     */
    public static @NotNull KeyframeDatapointVector of(@NotNull KeyframeDatapoint x, @NotNull KeyframeDatapoint y, @NotNull KeyframeDatapoint z) {
        return new KeyframeDatapointVector(x, y, z);
    }

    /**
     * Compiles each component with the same animation time. Expression compilation can fail in the active evaluator.
     * <pre>{@code
     * var vector = KeyframeDatapointVector.ZERO.compile(new KeyframeCompileContext(0F));
     * }</pre>
     *
     * @param context the compilation context for all three components
     * @return the compiled vector
     * @throws RuntimeException if the evaluator fails during compilation
     * @since 3.7.0
     */
    public @NotNull KeyframeVector compile(@NotNull KeyframeCompileContext context) {
        return KeyframeVector.of(
            x.compile(context),
            y.compile(context),
            z.compile(context)
        );
    }
}
