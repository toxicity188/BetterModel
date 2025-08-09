package kr.toxicity.model.api.util.interpolation;

import kr.toxicity.model.api.animation.VectorPoint;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

import static kr.toxicity.model.api.util.InterpolationUtil.*;

/**
 * Bezier interpolator
 */
@RequiredArgsConstructor
@ApiStatus.Internal
public final class BezierInterpolation implements VectorInterpolation {

    private final @Nullable Vector3f bezierLeftTime, bezierLeftValue, bezierRightTime, bezierRightValue;

    @NotNull
    @Override
    public Vector3f interpolate(@NotNull List<VectorPoint> points, int p2Index, float time) {
        var p1 = p2Index > 0 ? points.get(p2Index - 1) : VectorPoint.EMPTY;
        var p2 = points.get(p2Index);

        var t1 = p1.time();
        var t2 = p2.time();
        var a = alpha(t1, t2, time);
        var ha = a / 2F;

        return bezier(
                time,
                t1,
                t2,
                p1.vector(lerp(t1, t2, ha)),
                p2.vector(lerp(t1, t2, 1 - ha)),
                bezierLeftTime,
                bezierLeftValue,
                bezierRightTime,
                bezierRightValue
        );
    }
}
