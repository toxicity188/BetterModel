package kr.toxicity.model.api.animation;

import kr.toxicity.model.api.tracker.Tracker;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.SequencedMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@RequiredArgsConstructor
public final class AnimationStateHandler<T extends Timed> {
    
    private final AnimationCreator<T> initialCreator;
    private final AnimationMapper<T> mapper;
    private final Consumer<T> setConsumer;

    private final SequencedMap<String, TreeIterator> animators = new LinkedHashMap<>();
    private final Collection<TreeIterator> reversedView = animators.sequencedValues().reversed();
    private final AtomicBoolean forceUpdateAnimation = new AtomicBoolean(true);

    @Getter
    @Setter
    private int delay;
    private volatile TreeIterator currentIterator = null;
    @Getter
    private volatile T keyframe = null;

    public boolean keyframeFinished() {
        return delay <= 0;
    }

    public @Nullable RunningAnimation runningAnimation() {
        var iterator = currentIterator;
        return iterator != null ? iterator.animation : null;
    }

    public void tick() {
        delay--;
    }

    public boolean shouldUpdateAnimation() {
        return forceUpdateAnimation.compareAndSet(true, false) || keyframeFinished() || delay % Tracker.MINECRAFT_TICK_MULTIPLIER == 0;
    }

    public boolean updateAnimation() {
        synchronized (animators) {
            var iterator = reversedView.iterator();
            while (iterator.hasNext()) {
                var next = iterator.next();
                if (!next.getAsBoolean()) continue;
                if (currentIterator == null) {
                    if (updateKeyframe(iterator, next)) {
                        currentIterator = next;
                        return true;
                    }
                } else if (currentIterator != next) {
                    if (updateKeyframe(iterator, next)) {
                        currentIterator.clear();
                        currentIterator = next;
                        delay = 0;
                        return true;
                    }
                } else if (keyframeFinished()) {
                    if (updateKeyframe(iterator, next)) {
                        return true;
                    }
                } else {
                    return false;
                }
            }
        }
        return setKeyframe(null);
    }

    private boolean updateKeyframe(@NotNull Iterator<TreeIterator> iterator, @NotNull TreeIterator next) {
        if (!next.hasNext()) {
            next.run();
            iterator.remove();
            return false;
        } else {
            return setKeyframe(next.next());
        }
    }


    private boolean setKeyframe(@Nullable T next) {
        if (keyframe == next) return false;
        setConsumer.accept(keyframe = next);
        delay = Math.round(frame());
        return true;
    }

    public void addAnimation(@NotNull String name, @NotNull AnimationIterator<T> iterator, @NotNull AnimationModifier modifier, @NotNull Runnable removeTask) {
        synchronized (animators) {
            animators.putLast(name, new TreeIterator(name, iterator, modifier, removeTask));
        }
        forceUpdateAnimation.set(true);
    }

    public void replaceAnimation(@NotNull String name, @NotNull AnimationIterator<T> iterator, @NotNull AnimationModifier modifier) {
        synchronized (animators) {
            var v = animators.get(name);
            if (v != null) animators.replace(name, new TreeIterator(name, iterator, v.modifier, v.removeTask));
            else animators.replace(name, new TreeIterator(name, iterator, modifier, () -> {}));
        }
        forceUpdateAnimation.set(true);
    }

    public void stopAnimation(@NotNull String name) {
        synchronized (animators) {
            if (animators.remove(name) != null) forceUpdateAnimation.set(true);
        }
    }

    public float frame() {
        return keyframe != null ? keyframe.time() * 20F * Tracker.MINECRAFT_TICK_MULTIPLIER : 0F;
    }

    @FunctionalInterface
    public interface AnimationCreator<T extends Timed> {
        @NotNull T create(float time);
    }
    
    @FunctionalInterface
    public interface AnimationMapper<T extends Timed> {
        @NotNull T map(T t, float time);
    }

    private class TreeIterator implements BooleanSupplier, Runnable {
        private final RunningAnimation animation;
        private final AnimationIterator<T> iterator;
        private final AnimationModifier modifier;
        private final Runnable removeTask;

        private final T previous;

        private boolean started = false;
        private boolean ended = false;

        public TreeIterator(String name, AnimationIterator<T> iterator, AnimationModifier modifier, Runnable removeTask) {
            animation = new RunningAnimation(name, iterator.type());
            this.iterator = iterator;
            this.modifier = modifier;
            this.removeTask = removeTask;

            previous = keyframe != null ? keyframe : initialCreator.create((float) modifier.end() / 20);
        }

        @Override
        public void run() {
            removeTask.run();
        }

        @Override
        public boolean getAsBoolean() {
            return modifier.predicate().getAsBoolean();
        }

        public boolean hasNext() {
            return iterator.hasNext() || (modifier.end() > 0 && !ended);
        }

        public @NotNull T next() {
            if (!started) {
                started = true;
                return mapper.map(iterator.next(), (float) modifier.start() / 20);
            }
            if (!iterator.hasNext()) {
                ended = true;
                return mapper.map(previous, (float) modifier.end() / 20);
            }
            var nxt = iterator.next();
            return mapper.map(nxt, nxt.time() / modifier.speedValue());
        }

        public void clear() {
            iterator.clear();
            started = ended = !iterator.hasNext();
        }
    }
}
