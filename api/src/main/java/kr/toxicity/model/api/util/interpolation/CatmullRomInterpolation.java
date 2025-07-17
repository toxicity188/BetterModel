package kr.toxicity.model.api.util.interpolation;

import kr.toxicity.model.api.animation.VectorPoint;
import kr.toxicity.model.api.util.InterpolationUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Catmull-rom interpolator
 */
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
    public VectorPoint interpolate(@NotNull List<VectorPoint> points, int p2Index, float time) {
        var p0 = indexOf(points, p2Index, -2);
        var p1 = indexOf(points, p2Index, -1);
        var p2 = points.get(p2Index);
        var p3 = indexOf(points, p2Index, 1);
        //var p3 = next.time() == 0 ? indexOf(points, p2Index, 2) : next;
        return new VectorPoint(
                InterpolationUtil.catmull_rom(
                        p0.vector(),
                        p1.vector(),
                        p2.vector(),
                        p3.vector(),
                        InterpolationUtil.alpha(p1.time(), p2.time(), time)
                ),
                time
                ,this
        );
    }
}
