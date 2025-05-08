package kr.toxicity.model.api.util;

import kr.toxicity.model.api.animation.VectorPoint;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@ApiStatus.Internal
public enum VectorInterpolation {
    LINEAR {
        @NotNull
        @Override
        public VectorPoint interpolate(@NotNull List<VectorPoint> points, int p2Index, float time) {
            var p1 = p2Index > 0 ? points.get(p2Index - 1) : VectorPoint.EMPTY;
            var p2 = points.get(p2Index);
            return new VectorPoint(
                    VectorUtil.linear(p1.vector(), p2.vector(), VectorUtil.alpha(p1.time(), p2.time(), time)),
                    time,
                    this
            );
        }
    },
    CATMULLROM {

        private @NotNull VectorPoint indexOf(@NotNull List<VectorPoint> list, int index, int relative) {
            var i = index + relative;
            if (i < 0) i += list.size();
            else if (i >= list.size()) {
                i = list.getFirst().time() <= 0F ? i % (list.size() - 1) + 1 : i % list.size();
            }
            return list.get(i);
        }

        @NotNull
        @Override
        public VectorPoint interpolate(@NotNull List<VectorPoint> points, int p2Index, float time) {
            if (points.size() < 4) {
                return LINEAR.interpolate(points, p2Index, time);
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
    ;

    public abstract @NotNull VectorPoint interpolate(@NotNull List<VectorPoint> points, int p2Index, float time);

    public static @NotNull VectorInterpolation find(@Nullable String name) {
        if (name == null) return LINEAR;
        try {
            return valueOf(name.toUpperCase());
        } catch (Exception e) {
            return LINEAR;
        }
    }
}