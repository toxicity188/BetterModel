package kr.toxicity.model.api.util.interpolation;

import kr.toxicity.model.api.animation.VectorPoint;
import kr.toxicity.model.api.util.VectorUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public enum CatmullRomInterpolation implements VectorInterpolation {
    INSTANCE
    ;
    
    private static @NotNull VectorPoint indexOf(@NotNull List<VectorPoint> list, int index, int relative) {
        var i = index + relative;
        if (i < 0) i += list.size();
        else if (i >= list.size()) {
            i %= list.size();
        }
        return list.get(i);
    }
    
    @NotNull
    @Override
    public VectorPoint interpolate(@NotNull List<VectorPoint> points, int p2Index, float time) {
        if (points.size() < 4) {
            return VectorInterpolation.defaultInterpolation().interpolate(points, p2Index, time);
        }
        var p0 = indexOf(points, p2Index, -2);
        var p1 = indexOf(points, p2Index, -1);
        var p2 = points.get(p2Index);
        var p3 = indexOf(points, p2Index, 1);
        return new VectorPoint(
                VectorUtil.catmull_rom(
                        p0.vector(),
                        p1.vector(),
                        p2.vector(),
                        p3.vector(),
                        VectorUtil.alpha(p1.time(), p2.time(), time)
                ),
                time
                ,this
        );
    }
}
