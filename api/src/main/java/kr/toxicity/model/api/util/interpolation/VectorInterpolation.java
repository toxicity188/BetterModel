package kr.toxicity.model.api.util.interpolation;

import kr.toxicity.model.api.animation.VectorPoint;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Interpolator
 */
@ApiStatus.Internal
public interface VectorInterpolation {
    /**
     * Interpolates vector
     * @param points points
     * @param p2Index p2 index
     * @param time destination time
     * @return vector point
     */
    @NotNull VectorPoint interpolate(@NotNull List<VectorPoint> points, int p2Index, float time);

    /**
     * Gets default interpolator
     * @return default interpolator
     */
    static @NotNull VectorInterpolation defaultInterpolation() {
        return LinearInterpolation.INSTANCE;
    }
}
