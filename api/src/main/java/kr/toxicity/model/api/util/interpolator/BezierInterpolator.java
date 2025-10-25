/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.util.interpolator;

import kr.toxicity.model.api.animation.VectorPoint;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

import static kr.toxicity.model.api.util.InterpolationUtil.*;

/**
 * Bezier interpolator
 */
@ApiStatus.Internal
public record BezierInterpolator(
        @Nullable Vector3f bezierLeftTime,
        @Nullable Vector3f bezierLeftValue,
        @Nullable Vector3f bezierRightTime,
        @Nullable Vector3f bezierRightValue
) implements VectorInterpolator {

    @NotNull
    @Override
    public Vector3f interpolate(@NotNull List<VectorPoint> points, int p2Index, float time) {
        var p1 = p2Index > 0 ? points.get(p2Index - 1) : points.getFirst();
        var p2 = points.get(p2Index);

        var t1 = p1.time();
        var t2 = p2.time();
        var a = alpha(t1, t2, time);

        return bezier(
                time,
                t1,
                t2,
                p1.vector(lerp(t1, t2, a)),
                p2.vector(),
                bezierLeftTime,
                bezierLeftValue,
                bezierRightTime,
                bezierRightValue
        );
    }
}
