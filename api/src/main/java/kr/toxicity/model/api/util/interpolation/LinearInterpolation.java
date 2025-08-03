package kr.toxicity.model.api.util.interpolation;

import kr.toxicity.model.api.animation.VectorPoint;
import kr.toxicity.model.api.util.InterpolationUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.List;

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
        return InterpolationUtil.lerp(
                p1.vector(time),
                p2.vector(time),
                InterpolationUtil.alpha(p1.time(), p2.time(), time)
        );
    }
}
