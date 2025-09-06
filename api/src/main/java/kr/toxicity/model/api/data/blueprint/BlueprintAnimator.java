package kr.toxicity.model.api.data.blueprint;

import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.animation.AnimationMovement;
import kr.toxicity.model.api.animation.VectorPoint;
import kr.toxicity.model.api.data.raw.ModelKeyframe;
import kr.toxicity.model.api.data.raw.ModelPlaceholder;
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
     * @param position position
     * @param scale scale
     * @param rotation rotation
     */
    public record AnimatorData(
            @NotNull String name,
            @NotNull List<VectorPoint> position,
            @NotNull List<VectorPoint> scale,
            @NotNull List<VectorPoint> rotation
    ) {
        /**
         * Gets flatten points
         * @return all points
         */
        public @NotNull Stream<VectorPoint> allPoints() {
            return Stream.concat(
                    Stream.concat(
                            position.stream(),
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
         * @param placeholder placeholder
         */
        public void addFrame(@NotNull ModelKeyframe keyframe, @NotNull ModelPlaceholder placeholder) {
            var time = keyframe.time();
            if (time > length) return;
            var interpolation = keyframe.findInterpolator();
            var function = keyframe.dataPoints().getFirst().toFunction(placeholder);
            switch (keyframe.channel()) {
                case POSITION -> transform.add(new VectorPoint(
                        function.map(vec -> MathUtil.transformToDisplay(vec).div(MathUtil.MODEL_TO_BLOCK_MULTIPLIER)).memoize(),
                        time,
                        interpolation
                ));
                case ROTATION -> rotation.add(new VectorPoint(
                        function.map(MathUtil::animationToDisplay).memoize(),
                        time,
                        interpolation
                ));
                case SCALE -> scale.add(new VectorPoint(
                        function.map(vec -> vec.sub(1, 1, 1)).memoize(),
                        time,
                        interpolation
                ));
            }
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
