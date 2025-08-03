package kr.toxicity.model.api.util.interpolation;

import kr.toxicity.model.api.animation.VectorPoint;
import kr.toxicity.model.api.util.InterpolationUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.List;

/**
 * Catmull-rom interpolator
 */
@ApiStatus.Internal
public enum CatmullRomInterpolation implements VectorInterpolation {
    /**
     * Singleton
     */
    INSTANCE
    ;
    
    private static @NotNull VectorPoint indexOf(@NotNull List<VectorPoint> list, int index, int relative) {
        var i = index + relative;
        while (i < 0) i += list.size();
        return list.get(i % list.size());
    }
    
    @NotNull
    @Override
    public Vector3f interpolate(@NotNull List<VectorPoint> points, int p2Index, float time) {
        var p0 = indexOf(points, p2Index, -2);
        var p1 = indexOf(points, p2Index, -1);
        var p2 = points.get(p2Index);
        var p3 = indexOf(points, p2Index, 1);
        return InterpolationUtil.catmull_rom(
                p0.vector(),
                p1.vector(),
                p2.vector(time),
                p3.vector(),
                InterpolationUtil.alpha(p1.time(), p2.time(), time)
        );
    }
}
