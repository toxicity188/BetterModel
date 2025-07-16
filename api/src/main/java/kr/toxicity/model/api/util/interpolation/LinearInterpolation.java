package kr.toxicity.model.api.util.interpolation;

import kr.toxicity.model.api.animation.VectorPoint;
import kr.toxicity.model.api.util.InterpolationUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Linear interpolator
 */
public enum LinearInterpolation implements VectorInterpolation {
    /**
     * Singleton
     */
    INSTANCE
    ;
    @NotNull
    @Override
    public VectorPoint interpolate(@NotNull List<VectorPoint> points, int p2Index, float time) {
        var p1 = p2Index > 0 ? points.get(p2Index - 1) : VectorPoint.EMPTY;
        var p2 = points.get(p2Index);
        return new VectorPoint(
                InterpolationUtil.lerp(p1.vector(), p2.vector(), InterpolationUtil.alpha(p1.time(), p2.time(), time)),
                time,
                this
        );
    }
}
