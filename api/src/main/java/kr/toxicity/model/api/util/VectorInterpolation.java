package kr.toxicity.model.api.util;

import kr.toxicity.model.api.animation.VectorPoint;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

@ApiStatus.Internal
public enum VectorInterpolation {
    LINEAR {
        @NotNull
        @Override
        public Vector3f interpolate(@NotNull Vector3f p0, @NotNull Vector3f p1, float alpha) {
            return VectorUtil.linear(p0, p1, alpha);
        }

        @NotNull
        @Override
        public VectorPoint interpolate(@NotNull List<VectorPoint> points, int p2Index, float time) {
            var p1 = p2Index > 0 ? points.get(p2Index - 1) : VectorPoint.EMPTY;
            var p2 = points.get(p2Index);
            return new VectorPoint(
                    interpolate(p1.vector(), p2.vector(), VectorUtil.alpha(p1.time(), p2.time(), time)),
                    time,
                    this
            );
        }
    },
    CATMULLROM {
        @NotNull
        @Override
        public Vector3f interpolate(@NotNull Vector3f p0, @NotNull Vector3f p1, float alpha) {
            return VectorUtil.linear(p0, p1, alpha);
        }

        @NotNull
        @Override
        public VectorPoint interpolate(@NotNull List<VectorPoint> points, int p2Index, float time) {
            if (p2Index >= points.size() - 1 || p2Index < 2) {
                var p1 = p2Index > 0 ? points.get(p2Index - 1) : VectorPoint.EMPTY;
                var p2 = points.get(p2Index);
                return new VectorPoint(
                        interpolate(p1.vector(), p2.vector(), VectorUtil.alpha(p1.time(), p2.time(), time)),
                        time,
                        this
                );
            }
            var p1 = points.get(p2Index - 1);
            var p2 = points.get(p2Index);
            var p0 = points.get(p2Index - 2);
            var p3 = points.get(p2Index + 1);
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

    public abstract @NotNull Vector3f interpolate(@NotNull Vector3f p0, @NotNull Vector3f p1, float alpha);
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