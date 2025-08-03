package kr.toxicity.model.api.util.interpolation;

import kr.toxicity.model.api.animation.VectorPoint;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.List;

import static kr.toxicity.model.api.util.InterpolationUtil.alpha;
import static kr.toxicity.model.api.util.InterpolationUtil.lerp;

/**
 * Linear interpolator
 */
@ApiStatus.Internal
public enum LinearInterpolation implements VectorInterpolation {
    /**
     * Singleton
     */
    INSTANCE
    ;
    @NotNull
    @Override
    public Vector3f interpolate(@NotNull List<VectorPoint> points, int p2Index, float time) {
        var p1 = p2Index > 0 ? points.get(p2Index - 1) : VectorPoint.EMPTY;
        var p2 = points.get(p2Index);
        var t1 = p1.time();
        var t2 = p2.time();
        var a = alpha(t1, t2, time);
        return lerp(
                p1.vector(lerp(t1, t2, 1 - a)),
                p2.vector(lerp(t1, t2, a)),
                a
        );
    }
}
