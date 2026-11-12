/*
 * This source file is part of BetterModel.
 * Copyright (c) 2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.util.interpolator;

import com.google.gson.annotations.SerializedName;
import kr.toxicity.model.api.animation.keyframe.KeyframePoint;
import kr.toxicity.model.api.animation.keyframe.KeyframeVector;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

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
        public KeyframeVector interpolate(@NotNull List<KeyframePoint> points, int p2Index, float time) {
            var p1 = p2Index > 0 ? points.get(p2Index - 1) : points.getFirst();
            var p2 = points.get(p2Index);
            var t1 = p1.time();
            var t2 = p2.time();
            var a = alpha(t1, t2, time);

            return KeyframeVector.operate(
                p1.vector(lerp(t1, t2, a)),
                p2.vector(),
                (a1, a2) -> lerp(a1, a2, a)
            );
        }
    },
    /**
     * Catmullrom
     */
    @SerializedName("catmullrom")
    CATMULLROM {
        private static @NotNull KeyframePoint indexOf(@NotNull List<KeyframePoint> list, int index, int relative) {
            var i = index + relative;
            while (i < 0) i += list.size();
            return list.get(i % list.size());
        }

        @NotNull
        @Override
        public KeyframeVector interpolate(@NotNull List<KeyframePoint> points, int p2Index, float time) {
            var p0 = indexOf(points, p2Index, -2);
            var p1 = indexOf(points, p2Index, -1);
            var p2 = points.get(p2Index);
            var p3 = indexOf(points, p2Index, 1);

            var t1 = p1.time();
            var t2 = p2.time();
            var a = alpha(t1, t2, time);

            return KeyframeVector.operate(
                p0.vector(),
                p1.vector(lerp(t1, t2, a)),
                p2.vector(),
                p3.vector(),
                (a0, a1, a2, a3) -> catmull_rom(a0, a1, a2, a3, a)
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
        public KeyframeVector interpolate(@NotNull List<KeyframePoint> points, int p2Index, float time) {
            var p1 = p2Index > 0 ? points.get(p2Index - 1) : points.getFirst();
            var p2 = points.get(p2Index);

            var t1 = p1.time();
            var t2 = p2.time();
            var a = alpha(t1, t2, time);

            var rightTime = p1.bezier().rightTime();
            var rightValue = p1.bezier().rightTime();
            var leftTime = p2.bezier().leftTime();
            var leftValue = p2.bezier().leftValue();

            return KeyframeVector.operate(
                p1.vector(lerp(t1, t2, a)),
                p2.vector(),
                (x1, x2) -> bezier(a, x1, x2, rightTime.x, rightValue.x, leftTime.x, leftValue.x),
                (y1, y2) -> bezier(a, y1, y2, rightTime.y, rightValue.y, leftTime.y, leftValue.y),
                (z1, z2) -> bezier(a, z1, z2, rightTime.z, rightValue.z, leftTime.z, leftValue.z)
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
        public KeyframeVector interpolate(@NotNull List<KeyframePoint> points, int p2Index, float time) {
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
    public abstract KeyframeVector interpolate(@NotNull List<KeyframePoint> points, int p2Index, float time);

    /**
     * Checks this interpolator is continuous
     * @return is continuous
     */
    public boolean isContinuous() {
        return true;
    }
}
