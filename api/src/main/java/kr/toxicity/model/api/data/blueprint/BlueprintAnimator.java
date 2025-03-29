package kr.toxicity.model.api.data.blueprint;

import kr.toxicity.model.api.animation.AnimationMovement;
import kr.toxicity.model.api.animation.AnimationPoint;
import kr.toxicity.model.api.animation.VectorPoint;
import kr.toxicity.model.api.data.raw.Datapoint;
import kr.toxicity.model.api.data.raw.ModelKeyframe;
import kr.toxicity.model.api.util.*;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A movement of each group.
 * @param name group name
 * @param length frame length
 * @param keyFrame keyframes
 */
public record BlueprintAnimator(@NotNull String name, float length, @NotNull @Unmodifiable List<AnimationMovement> keyFrame) {


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

        private static int checkSplit(float angle) {
            return (int) Math.floor(Math.toDegrees(angle) / 45) + 1;
        }

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
                    case ROTATION -> {
                        var rot = new VectorPoint(
                                MathUtil.animationToDisplay(vec),
                                keyframe.time(),
                                interpolation
                        );
                        if (!rotation.isEmpty() && rot.time() > 0) {
                            var last = rotation.getLast();
                            var split = checkSplit(
                                    MathUtil.toQuaternion(MathUtil.blockBenchToDisplay(rot.vector()))
                                            .mul(MathUtil.toQuaternion(MathUtil.blockBenchToDisplay(last.vector())).invert())
                                            .angle()
                            );
                            if (split > 1) {
                                for (int i = 1; i < split; i++) {
                                    var alpha = (float) i / (float) split;
                                    rotation.add(new VectorPoint(
                                            VectorUtil.linear(last.vector(), rot.vector(), alpha),
                                            VectorUtil.linear(last.time(), rot.time(), alpha),
                                            interpolation
                                    ));
                                }
                            }
                        }
                        rotation.add(rot);
                    }
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
     * Gets single iterator.
     * @return iterator
     */
    public @NotNull AnimatorIterator singleIterator() {
        return new SingleIterator();
    }

    /**
     * Gets loop iterator.
     * @return iterator
     */
    public @NotNull AnimatorIterator loopIterator() {
        return new LoopIterator();
    }

    /**
     * A keyframe iterator of animation.
     */
    public interface AnimatorIterator extends Iterator<AnimationMovement> {
        /**
         * Gets first element of animation.
         * @return first element
         */
        @NotNull AnimationMovement first();

        /**
         * Clears this iterator
         */
        void clear();

        /**
         * Gets current index
         * @return current index
         */
        int index();

        /**
         * Gets last index of animator
         * @return last index
         */
        int lastIndex();
    }


    private class SingleIterator implements AnimatorIterator {

        private int index = 0;

        @NotNull
        @Override
        public AnimationMovement first() {
            return keyFrame.getFirst();
        }

        @Override
        public int index() {
            return index;
        }

        @Override
        public int lastIndex() {
            return keyFrame.size() - 1;
        }

        @Override
        public void clear() {
            index = Integer.MAX_VALUE;
        }

        @Override
        public boolean hasNext() {
            return index < keyFrame.size();
        }

        @Override
        public AnimationMovement next() {
            return keyFrame.get(index++);
        }
    }
    private class LoopIterator implements AnimatorIterator {

        private int index = 0;

        @NotNull
        @Override
        public AnimationMovement first() {
            return keyFrame.getFirst();
        }

        @Override
        public int index() {
            return index;
        }

        @Override
        public int lastIndex() {
            return keyFrame.size() - 1;
        }

        @Override
        public void clear() {
            index = 0;
        }

        @Override
        public boolean hasNext() {
            return true;
        }

        @Override
        public AnimationMovement next() {
            if (index >= keyFrame.size()) index = 0;
            return keyFrame.get(index++);
        }
    }
}
