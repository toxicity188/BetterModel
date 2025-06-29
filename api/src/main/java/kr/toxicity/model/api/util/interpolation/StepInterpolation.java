package kr.toxicity.model.api.util.interpolation;

import kr.toxicity.model.api.animation.VectorPoint;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Step interpolator
 */
public enum StepInterpolation implements VectorInterpolation {
    /**
     * Singleton
     */
    INSTANCE
    ;

    @NotNull
    @Override
    public VectorPoint interpolate(@NotNull List<VectorPoint> points, int p2Index, float time) {
        return new VectorPoint(
                (p2Index > 0 ? points.get(p2Index - 1) : VectorPoint.EMPTY).vector(),
                time,
                this
        );
    }
}
