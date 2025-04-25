package kr.toxicity.model.api.data.blueprint;

import kr.toxicity.model.api.animation.AnimationMovement;
import kr.toxicity.model.api.animation.AnimationPoint;
import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.animation.VectorPoint;
import kr.toxicity.model.api.data.raw.Datapoint;
import kr.toxicity.model.api.data.raw.ModelKeyframe;
import kr.toxicity.model.api.util.*;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * A movement of each group.
 * @param name group name
 * @param length frame length
 * @param keyFrame keyframes
 */
public record BlueprintAnimator(
        @NotNull String name,
        float length,
        @NotNull @Unmodifiable List<AnimationMovement> keyFrame
) {


    /**
     * Animation data
     * @param name name
     * @param length length
     * @param points points
     */
    public record AnimatorData(@NotNull String name, float length, @NotNull List<AnimationPoint> points) {}

    /**
     * Builder
     */
    @RequiredArgsConstructor
    public static final class Builder {

        private final float length;

        private final List<VectorPoint> transform = new ArrayList<>();
        private final List<VectorPoint> scale = new ArrayList<>();
        private final List<VectorPoint> rotation = new ArrayList<>();

        /**
         * Adds raw model frame.
         * @param keyframe raw frame
         * @return self
         */
        public @NotNull Builder addFrame(@NotNull ModelKeyframe keyframe) {
            if (keyframe.time() > length) return this;
            var interpolation = VectorInterpolation.find(keyframe.interpolation());
            for (Datapoint dataPoint : keyframe.dataPoints()) {
                var vec = dataPoint.toVector();
                switch (keyframe.channel()) {
                    case POSITION -> transform.add(new VectorPoint(
                            MathUtil.transformToDisplay(vec.div(16)),
                            keyframe.time(),
                            interpolation
                    ));
                    case ROTATION -> rotation.add(new VectorPoint(
                            MathUtil.animationToDisplay(vec),
                            keyframe.time(),
                            interpolation
                    ));
                    case SCALE -> scale.add(new VectorPoint(
                            vec.sub(1, 1, 1),
                            keyframe.time(),
                            interpolation
                    ));
                }
            }
            return this;
        }

        private void addLastFrame(@NotNull List<VectorPoint> points) {
            VectorPoint lastPoint;
            if (points.isEmpty()) {
                lastPoint = new VectorPoint(
                        new Vector3f(),
                        length,
                        VectorInterpolation.LINEAR
                );
            } else {
                var last = points.getLast();
                if (last.time() == length) return;
                lastPoint = new VectorPoint(
                        last.vector(),
                        length,
                        last.interpolation()
                );
            }
            points.add(lastPoint);
        }

        /**
         * Builds animation data
         * @param name animation's name
         * @return data
         */
        public @NotNull AnimatorData build(@NotNull String name) {
            addLastFrame(transform);
            addLastFrame(rotation);
            addLastFrame(scale);
            return new AnimatorData(name, length, VectorUtil.sum(
                    transform.stream().distinct().toList(),
                    rotation.stream().distinct().toList(),
                    scale.stream().distinct().toList()
            ));
        }
    }

    /**
     * Gets loop iterator.
     * @param type type
     * @return iterator
     */
    public @NotNull AnimationIterator iterator(@NotNull AnimationIterator.Type type) {
        return type.create(keyFrame);
    }
}
