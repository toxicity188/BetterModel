package kr.toxicity.model.api.data.blueprint;

import kr.toxicity.model.api.data.raw.Datapoint;
import kr.toxicity.model.api.data.raw.ModelKeyframe;
import kr.toxicity.model.api.util.MathUtil;
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

    private record TimeVector(float time, @NotNull Vector3f vector3f) {
        private static final TimeVector EMPTY = new TimeVector(0, new Vector3f());
    }

    /**
     * Builder
     */
    @RequiredArgsConstructor
    public static final class Builder {

        private final float length;

        private final List<TimeVector> transform = new ArrayList<>();
        private final List<TimeVector> scale = new ArrayList<>();
        private final List<TimeVector> rotation = new ArrayList<>();

        private static int checkSplit(float angle) {
            return (int) Math.floor(Math.toDegrees(angle) / 45) + 1;
        }

        /**
         * Adds raw model frame.
         * @param keyframe raw frame
         * @return self
         */
        public @NotNull Builder addFrame(@NotNull ModelKeyframe keyframe) {
            for (Datapoint dataPoint : keyframe.dataPoints()) {
                var vec = dataPoint.toVector();
                switch (keyframe.channel()) {
                    case POSITION -> transform.add(new TimeVector(keyframe.time(), MathUtil.transformToDisplay(vec.div(16))));
                    case ROTATION -> {
                        var rot = new TimeVector(keyframe.time(), MathUtil.animationToDisplay(vec));
                        var last = rotation.isEmpty() ? TimeVector.EMPTY : rotation.getLast();
                        var split = checkSplit(
                                MathUtil.toQuaternion(MathUtil.blockBenchToDisplay(rot.vector3f))
                                        .mul(MathUtil.toQuaternion(MathUtil.blockBenchToDisplay(last.vector3f)).invert())
                                        .angle()
                        );
                        if (split > 1) {
                            for (int i = 1; i < split; i++) {
                                var t = (rot.time - last.time) / split * i + last.time;
                                rotation.add(new TimeVector(t, get(t, last, rot)));
                            }
                        }
                        rotation.add(rot);
                    }
                    case SCALE -> scale.add(new TimeVector(keyframe.time(), vec.sub(1, 1, 1)));
                }
            }
            return this;
        }

        private Vector3f get(float min, TimeVector last, TimeVector newVec) {
            if (newVec.time == 0 || newVec.time - last.time == 0) return newVec.vector3f;
            return new Vector3f(last.vector3f)
                    .add(new Vector3f(newVec.vector3f).sub(last.vector3f).mul(min - last.time).div(newVec.time - last.time));
        }

        private @NotNull List<AnimationMovement> toAnimation() {
            var list = new ArrayList<AnimationMovement>();
            var ti = 0;
            var si = 0;
            var ri = 0;
            var beforeTime = 0F;

            var t = !transform.isEmpty() ? transform.getFirst() : null;
            var s = !scale.isEmpty() ? scale.getFirst() : null;
            var r = !rotation.isEmpty() ? rotation.getFirst() : null;

            TimeVector lastP = TimeVector.EMPTY, lastS = TimeVector.EMPTY, lastR = TimeVector.EMPTY;

            while (t != null || s != null || r != null) {
                var min = Math.min(t != null ? t.time : Long.MAX_VALUE, Math.min(s != null ? s.time : Long.MAX_VALUE, r != null ? r.time : Long.MAX_VALUE));
                list.add(new AnimationMovement(
                        min,
                        t != null ? get(min, lastP, t) : lastP.vector3f,
                        s != null ? get(min, lastS, s) : lastS.vector3f,
                        r != null ? get(min, lastR, r) : lastR.vector3f
                ));
                if (t != null && t.time <= min) {
                    lastP = t;
                    ti++;
                }
                if (s != null && s.time <= min) {
                    lastS = s;
                    si++;
                }
                if (r != null && r.time <= min) {
                    lastR = r;
                    ri++;
                }
                t = transform.size() > ti ? transform.get(ti) : null;
                s = scale.size() > si ? scale.get(si) : null;
                r = rotation.size() > ri ? rotation.get(ri) : null;
                beforeTime = min;
            }
            if (beforeTime < length && !list.isEmpty()) {
                var last = list.getLast();
                list.add(new AnimationMovement(
                        length,
                        last.transform(),
                        last.scale(),
                        last.rotation()
                ));
            }
            return list.isEmpty() ? List.of(new AnimationMovement(1, null, null, null)) : list;
        }

        public @NotNull BlueprintAnimator build(@NotNull String name) {
            return new BlueprintAnimator(name, length, toAnimation());
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

    public interface AnimatorIterator extends Iterator<AnimationMovement> {
        @NotNull AnimationMovement first();
        void clear();
        int length();
        int index();
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
        public int length() {
            return Math.round(length * 20);
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
        public int length() {
            return Math.round(length * 20);
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
