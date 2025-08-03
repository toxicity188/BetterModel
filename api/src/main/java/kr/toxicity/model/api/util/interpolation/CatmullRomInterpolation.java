package kr.toxicity.model.api.util.interpolation;

import kr.toxicity.model.api.animation.VectorPoint;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.List;

import static kr.toxicity.model.api.util.InterpolationUtil.*;

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

        var t0 = p0.time();
        var t1 = p1.time();
        var t2 = p2.time();
        var t3 = p3.time();
        var a = alpha(t1, t2, time);

        return catmull_rom(
                p0.vector(lerp(t0, t1, 1 - a)),
                p1.vector(lerp(t1, t2, 1 - a)),
                p2.vector(lerp(t1, t2, a)),
                p3.vector(lerp(t2, t3, a)),
                a
        );
    }
}
