package kr.toxicity.model.api.util.interpolation;

import kr.toxicity.model.api.animation.VectorPoint;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface VectorInterpolation {
    @NotNull VectorPoint interpolate(@NotNull List<VectorPoint> points, int p2Index, float time);

    static @NotNull VectorInterpolation defaultInterpolation() {
        return LinearInterpolation.INSTANCE;
    }
}
