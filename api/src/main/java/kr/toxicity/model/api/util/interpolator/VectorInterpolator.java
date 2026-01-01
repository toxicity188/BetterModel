/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.util.interpolator;

import com.google.gson.annotations.SerializedName;
import kr.toxicity.model.api.animation.VectorPoint;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.List;

import static kr.toxicity.model.api.util.InterpolationUtil.*;

/**
 * Interpolator
 */
@ApiStatus.Internal
public enum VectorInterpolator {
    /**
     * Linear
     */
    @SerializedName("linear")
    LINEAR {
        @NotNull
        @Override
        public Vector3f interpolate(@NotNull List<VectorPoint> points, int p2Index, float time) {
            var p1 = p2Index > 0 ? points.get(p2Index - 1) : points.getFirst();
            var p2 = points.get(p2Index);
            var t1 = p1.time();
            var t2 = p2.time();
            var a = alpha(t1, t2, time);
            return lerp(
                p1.vector(lerp(t1, t2, a)),
                p2.vector(),
                a
            );
        }
    },
    /**
     * Catmullrom
     */
    @SerializedName("catmullrom")
    CATMULLROM {
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

            var t1 = p1.time();
            var t2 = p2.time();
            var a = alpha(t1, t2, time);

            return catmull_rom(
                p0.vector(),
                p1.vector(lerp(t1, t2, a)),
                p2.vector(),
                p3.vector(),
                a
            );
        }
    },
    /**
     * Bezier
     */
    @SerializedName("bezier")
    BEZIER {
        @NotNull
        @Override
        public Vector3f interpolate(@NotNull List<VectorPoint> points, int p2Index, float time) {
            var p1 = p2Index > 0 ? points.get(p2Index - 1) : points.getFirst();
            var p2 = points.get(p2Index);

            var t1 = p1.time();
            var t2 = p2.time();
            var a = alpha(t1, t2, time);

            return bezier(
                a,
                p1.vector(lerp(t1, t2, a)),
                p2.vector(),
                p1.bezier().rightTime(),
                p1.bezier().rightValue(),
                p2.bezier().leftTime(),
                p2.bezier().leftValue()
            );
        }
    },
    /**
     * Step
     */
    @SerializedName("step")
    STEP {
        @NotNull
        @Override
        public Vector3f interpolate(@NotNull List<VectorPoint> points, int p2Index, float time) {
            return (p2Index > 0 ? points.get(p2Index - 1) : points.getFirst()).vector(time);
        }

        @Override
        public boolean isContinuous() {
            return false;
        }
    }
    ;

    /**
     * Interpolates vector
     * @param points points
     * @param p2Index p2 index
     * @param time destination time
     * @return interpolated vector
     */
    @NotNull
    public abstract Vector3f interpolate(@NotNull List<VectorPoint> points, int p2Index, float time);

    /**
     * Checks this interpolator is continuous
     * @return is continuous
     */
    public boolean isContinuous() {
        return true;
    }
}
