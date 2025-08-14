package kr.toxicity.model.api.util.interpolator;

import kr.toxicity.model.api.animation.VectorPoint;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.List;

/**
 * Interpolator
 */
@ApiStatus.Internal
public interface VectorInterpolator {
    /**
     * Interpolates vector
     * @param points points
     * @param p2Index p2 index
     * @param time destination time
     * @return interpolated vector
     */
    @NotNull Vector3f interpolate(@NotNull List<VectorPoint> points, int p2Index, float time);

    /**
     * Checks this interpolator is continuous
     * @return is continuous
     */
    default boolean isContinuous() {
        return true;
    }

    /**
     * Gets default interpolator
     * @return default interpolator
     */
    static @NotNull VectorInterpolator defaultInterpolator() {
        return LinearInterpolator.INSTANCE;
    }
}
