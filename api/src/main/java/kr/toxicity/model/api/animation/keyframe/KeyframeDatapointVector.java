/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.animation.keyframe;

import org.jetbrains.annotations.NotNull;

public record KeyframeDatapointVector(
    @NotNull KeyframeDatapoint x,
    @NotNull KeyframeDatapoint y,
    @NotNull KeyframeDatapoint z
) {

    public static final KeyframeDatapointVector ZERO = of(KeyframeDatapoint.ZERO, KeyframeDatapoint.ZERO, KeyframeDatapoint.ZERO);

    public static @NotNull KeyframeDatapointVector of(@NotNull KeyframeDatapoint x, @NotNull KeyframeDatapoint y, @NotNull KeyframeDatapoint z) {
        return new KeyframeDatapointVector(x, y, z);
    }

    public @NotNull KeyframeVector compile(@NotNull KeyframeCompileContext context) {
        return KeyframeVector.of(
            x.compile(context),
            y.compile(context),
            z.compile(context)
        );
    }
}
