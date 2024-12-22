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

public record BlueprintAnimator(@NotNull String name, int length, @NotNull @Unmodifiable List<AnimationMovement> keyFrame) {

    private record TimeVector(long time, Vector3f vector3f) {
        private static final TimeVector EMPTY = new TimeVector(0, null);
    }

    @RequiredArgsConstructor
    public static final class Builder {

        private final int length;

        private final List<TimeVector> transform = new ArrayList<>();
        private final List<TimeVector> scale = new ArrayList<>();
        private final List<TimeVector> rotation = new ArrayList<>();

        private static int checkSplit(Vector3f vector3f) {
            return (int) (Math.max(
                    Math.abs(vector3f.x),
                    Math.max(Math.abs(vector3f.y), Math.abs(vector3f.z))
            ) / 90.0) + 1;
        }

        public @NotNull Builder addFrame(@NotNull ModelKeyframe keyframe) {
            for (Datapoint dataPoint : keyframe.dataPoints()) {
                var vec = dataPoint.toVector();
                switch (keyframe.channel()) {
                    case POSITION -> transform.add(new TimeVector(Math.round(keyframe.time() * 20), MathUtil.transformToDisplay(vec.div(16))));
                    case ROTATION -> {
                        var rot = new TimeVector(Math.round(keyframe.time() * 20), MathUtil.animationToDisplay(vec));
                        if (rotation.isEmpty()) {
                            rotation.add(rot);
                        } else {
                            var last = rotation.getLast();
                            var split = checkSplit(new Vector3f(rot.vector3f).sub(last.vector3f));
                            if (split > 1) {
                                for (int i = 1; i < split; i++) {
                                    var t = Math.round((double) (rot.time - last.time) / split * i) + last.time;
                                    rotation.add(new TimeVector(t, get(t, last, rot)));
                                }
                            }
                            rotation.add(rot);
                        }
                    }
                    case SCALE -> scale.add(new TimeVector(Math.round(keyframe.time() * 20), vec.sub(1, 1, 1)));
                }
            }
            return this;
        }

        private Vector3f get(long min, TimeVector last, TimeVector newVec) {
            if (newVec.time == 0) return newVec.vector3f;
            if (last.vector3f == null) {
                return new Vector3f(newVec.vector3f).mul(min).div(newVec.time);
            } else {
                return new Vector3f(last.vector3f)
                        .add(new Vector3f(newVec.vector3f).sub(last.vector3f).mul(min - last.time).div(newVec.time - last.time));
            }
        }

        private @NotNull List<AnimationMovement> toAnimation() {
            var list = new ArrayList<AnimationMovement>();
            var ti = 0;
            var si = 0;
            var ri = 0;
            var beforeTime = 0L;

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
            if (beforeTime < length) {
                var last = list.getLast();
                list.add(new AnimationMovement(
                        length,
                        last.transform(),
                        last.scale(),
                        last.rotation()
                ));
            }
            return list;
        }

        public @NotNull BlueprintAnimator build(@NotNull String name) {
            return new BlueprintAnimator(name, length, toAnimation());
        }
    }

    public @NotNull AnimatorIterator singleIterator() {
        return new SingleIterator();
    }
    public @NotNull AnimatorIterator loopIterator() {
        return new LoopIterator();
    }

    public interface AnimatorIterator extends Iterator<AnimationMovement> {
        void clear();
        int length();
        int index();
        int lastIndex();
    }


    private class SingleIterator implements AnimatorIterator {

        private int index = 0;

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
            return length;
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
            return length;
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
