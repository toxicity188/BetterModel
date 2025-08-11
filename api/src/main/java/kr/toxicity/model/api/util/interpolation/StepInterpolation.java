package kr.toxicity.model.api.util.interpolation;

import kr.toxicity.model.api.animation.VectorPoint;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.List;

/**
 * Step interpolator
 */
@ApiStatus.Internal
public enum StepInterpolation implements VectorInterpolation {
    /**
     * Singleton
     */
    INSTANCE
    ;

    @NotNull
    @Override
    public Vector3f interpolate(@NotNull List<VectorPoint> points, int p2Index, float time) {
        return (p2Index > 0 ? points.get(p2Index - 1) : VectorPoint.EMPTY).vector(time);
    }
}
