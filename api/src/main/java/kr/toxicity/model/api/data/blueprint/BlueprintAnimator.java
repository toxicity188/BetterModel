package kr.toxicity.model.api.data.blueprint;

import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.animation.AnimationMovement;
import kr.toxicity.model.api.animation.VectorPoint;
import kr.toxicity.model.api.data.raw.Datapoint;
import kr.toxicity.model.api.data.raw.ModelKeyframe;
import kr.toxicity.model.api.util.MathUtil;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * A movement of each group.
 * @param name group name
 * @param keyFrame keyframes
 */
public record BlueprintAnimator(
        @NotNull String name,
        @NotNull @Unmodifiable List<AnimationMovement> keyFrame
) {


    /**
     * Animation data
     * @param name name
     * @param transform transform
     * @param scale scale
     * @param rotation rotation
     */
    public record AnimatorData(
            @NotNull String name,
            @NotNull List<VectorPoint> transform,
            @NotNull List<VectorPoint> scale,
            @NotNull List<VectorPoint> rotation
    ) {
        public @NotNull Stream<VectorPoint> allPoints() {
            return Stream.concat(
                    Stream.concat(
                            transform.stream(),
                            scale.stream()
                    ),
                    rotation.stream()
            );
        }
    }

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
            var interpolation = keyframe.findInterpolation();
            for (Datapoint dataPoint : keyframe.dataPoints()) {
                var vec = dataPoint.toVector();
                switch (keyframe.channel()) {
                    case POSITION -> transform.add(new VectorPoint(
                            MathUtil.transformToDisplay(vec.div(MathUtil.MODEL_TO_BLOCK_MULTIPLIER)),
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

        /**
         * Builds animation data
         * @param name animation's name
         * @return data
         */
        public @NotNull AnimatorData build(@NotNull String name) {
            return new AnimatorData(
                    name,
                    transform,
                    scale,
                    rotation
            );
        }
    }

    /**
     * Gets loop iterator.
     * @param type type
     * @return iterator
     */
    public @NotNull AnimationIterator<AnimationMovement> iterator(@NotNull AnimationIterator.Type type) {
        return type.create(keyFrame);
    }
}
