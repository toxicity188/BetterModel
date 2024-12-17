package kr.toxicity.model.api.data.blueprint;

import kr.toxicity.model.api.data.raw.Datapoint;
import kr.toxicity.model.api.data.raw.ModelKeyframe;
import kr.toxicity.model.api.util.MathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public record BlueprintAnimator(@NotNull String name, @NotNull @Unmodifiable List<AnimationMovement> keyFrame) {

    private record TimeVector(long time, Vector3f vector3f) {
    }

    public static final class Builder {
        private final List<TimeVector> transform = new ArrayList<>();
        private final List<TimeVector> scale = new ArrayList<>();
        private final List<TimeVector> rotation = new ArrayList<>();

        public @NotNull Builder addFrame(@NotNull ModelKeyframe keyframe) {
            for (Datapoint dataPoint : keyframe.dataPoints()) {
                var vec = dataPoint.toVector();
                switch (keyframe.channel()) {
                    case POSITION -> transform.add(new TimeVector(Math.round(keyframe.time() * 20), vec.div(16)));
                    case ROTATION -> rotation.add(new TimeVector(Math.round(keyframe.time() * 20), MathUtil.animationToDisplay(vec)));
                    case SCALE -> scale.add(new TimeVector(Math.round(keyframe.time() * 20), vec));
                }
            }
            return this;
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
            while (t != null || s != null || r != null) {
                var min = Math.min(t != null ? t.time : Long.MAX_VALUE, Math.min(s != null ? s.time : Long.MAX_VALUE, r != null ? r.time : Long.MAX_VALUE));
                if (t != null && t.time <= min) ti++;
                if (s != null && s.time <= min) si++;
                if (r != null && r.time <= min) ri++;
                list.add(new AnimationMovement(
                        min - beforeTime,
                        t != null ? t.time == 0 ? t.vector3f : new Vector3f(t.vector3f).mul(min).div(t.time) : null,
                        s != null ? s.time == 0 ? s.vector3f : new Vector3f(s.vector3f).mul(min).div(s.time) : null,
                        r != null ? r.time == 0 ? r.vector3f : new Vector3f(r.vector3f).mul(min).div(r.time) : null
                ));
                t = transform.size() > ti ? transform.get(ti) : null;
                s = scale.size() > si ? scale.get(si) : null;
                r = rotation.size() > ri ? rotation.get(ri) : null;
                beforeTime = min;
            }
            return list;
        }

        public @NotNull BlueprintAnimator build(@NotNull String name) {
            return new BlueprintAnimator(name, toAnimation());
        }

        private static <T> T getOrNull(@NotNull List<T> list, int index) {
            return list.size() > index ? list.get(index) : null;
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
    }


    private class SingleIterator implements AnimatorIterator {
        private final Iterator<AnimationMovement> delegate = keyFrame.iterator();

        @Override
        public void clear() {

        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public AnimationMovement next() {
            return delegate.next();
        }
    }
    private class LoopIterator implements AnimatorIterator {

        private int index = 0;

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
