package kr.toxicity.model.api.animation;

import kr.toxicity.model.api.tracker.Tracker;
import kr.toxicity.model.api.util.MathUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.SequencedMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

/**
 * Animation state handler
 * @param <T> timed value
 */
@RequiredArgsConstructor
@ApiStatus.Internal
public final class AnimationStateHandler<T extends Timed> {
    
    private final T initialValue;
    private final AnimationMapper<T> mapper;
    private final BiConsumer<T, T> setConsumer;

    private final SequencedMap<String, TreeIterator> animators = new LinkedHashMap<>();
    private final Collection<TreeIterator> reversedView = animators.sequencedValues().reversed();
    private final AtomicBoolean forceUpdateAnimation = new AtomicBoolean();

    @Getter
    private int delay;
    private volatile TreeIterator currentIterator = null;
    @Getter
    private volatile T beforeKeyframe = null, afterKeyframe = null;

    /**
     * Checks this keyframe has been finished
     * @return finished
     */
    public boolean keyframeFinished() {
        return delay <= 0;
    }

    /**
     * Gets running animation
     * @return animation
     */
    public @Nullable RunningAnimation runningAnimation() {
        var iterator = currentIterator;
        return iterator != null ? iterator.animation : null;
    }

    /**
     * Ticks this state handler
     * @return keyframe has been shifted or not
     */
    public boolean tick() {
        return tick(() -> {});
    }

    /**
     * Ticks this state handler
     * @param ifEmpty callback if animator is empty
     * @return keyframe has been shifted or not
     */
    public boolean tick(@NotNull Runnable ifEmpty) {
        delay--;
        if (animators.isEmpty()) {
            ifEmpty.run();
            return false;
        }
        return shouldUpdateAnimation() && updateAnimation();
    }

    /**
     * Gets the progress of current keyframe
     * @return progress
     */
    public float progress() {
        var frame = frame();
        return frame == 0 ? 0 : (float) delay / frame;
    }

    private boolean shouldUpdateAnimation() {
        return forceUpdateAnimation.compareAndSet(true, false) || (afterKeyframe != null && keyframeFinished()) || delay % Tracker.MINECRAFT_TICK_MULTIPLIER == 0;
    }

    private boolean updateAnimation() {
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
        return setAfterKeyframe(null);
    }

    private boolean updateKeyframe(@NotNull Iterator<TreeIterator> iterator, @NotNull TreeIterator next) {
        if (!next.hasNext()) {
            next.eventHandler.animationRemove();
            iterator.remove();
            return false;
        } else {
            return setAfterKeyframe(next.next());
        }
    }


    private boolean setAfterKeyframe(@Nullable T next) {
        if (afterKeyframe == next) return false;
        setConsumer.accept(
                beforeKeyframe = afterKeyframe,
                afterKeyframe = next
        );
        delay = Math.round(frame());
        return true;
    }

    /**
     * Adds animation
     * @param name name
     * @param iterator iterator
     * @param modifier modifier
     * @param eventHandler event handler
     */
    public void addAnimation(@NotNull String name, @NotNull AnimationIterator<T> iterator, @NotNull AnimationModifier modifier, @NotNull AnimationEventHandler eventHandler) {
        synchronized (animators) {
            animators.putLast(name, new TreeIterator(name, iterator, modifier, eventHandler));
        }
        forceUpdateAnimation.set(true);
    }

    /**
     * Replaces animation
     * @param name name
     * @param iterator iterator
     * @param modifier modifier
     */
    public void replaceAnimation(@NotNull String name, @NotNull AnimationIterator<T> iterator, @NotNull AnimationModifier modifier) {
        synchronized (animators) {
            animators.computeIfPresent(name, (k, v) -> new TreeIterator(k, iterator, v.modifier.toBuilder()
                    .mergeNotDefault(modifier)
                    .build(), v.eventHandler));
        }
        forceUpdateAnimation.set(true);
    }

    /**
     * Remove animation
     * @param name name
     * @return success
     */
    public boolean stopAnimation(@NotNull String name) {
        synchronized (animators) {
            if (animators.remove(name) != null) {
                forceUpdateAnimation.set(true);
                return true;
            }
        }
        return false;
    }

    /**
     * Gets ticking frame of current keyframe
     * @return ticking frame
     */
    public float frame() {
        return afterKeyframe != null ? 20 * Tracker.MINECRAFT_TICK_MULTIPLIER * (afterKeyframe.time() + MathUtil.FRAME_EPSILON) : 0F;
    }

    /**
     * Animation mapper
     * @param <T> type
     */
    @FunctionalInterface
    public interface AnimationMapper<T extends Timed> {
        /**
         * Maps keyframe's time
         * @param keyframe keyframe
         * @param state state
         * @param time time
         * @return keyframe
         */
        @NotNull T map(T keyframe, @NotNull MappingState state, float time);
    }

    /**
     * Mapping state
     */
    public enum MappingState {
        /**
         * Start
         */
        START,
        /**
         * Progress
         */
        PROGRESS,
        /**
         * End
         */
        END
    }

    private class TreeIterator implements BooleanSupplier {
        private final RunningAnimation animation;
        private final AnimationIterator<T> iterator;
        private final AnimationModifier modifier;
        private final AnimationEventHandler eventHandler;

        private final T previous;

        private boolean started = false;
        private boolean ended = false;

        public TreeIterator(String name, AnimationIterator<T> iterator, AnimationModifier modifier, AnimationEventHandler eventHandler) {
            animation = new RunningAnimation(name, iterator.type());
            this.iterator = iterator;
            this.modifier = modifier;
            this.eventHandler = eventHandler;

            previous = afterKeyframe != null ? afterKeyframe : initialValue;
        }

        @Override
        public boolean getAsBoolean() {
            return modifier.predicateValue();
        }

        public boolean hasNext() {
            return iterator.hasNext() || (modifier.end() > 0 && !ended);
        }

        public @NotNull T next() {
            if (!started) {
                started = true;
                return mapper.map(iterator.next(), MappingState.START, (float) modifier.start() / 20);
            }
            if (!iterator.hasNext()) {
                ended = true;
                return mapper.map(previous, MappingState.END, (float) modifier.end() / 20);
            }
            var nxt = iterator.next();
            return mapper.map(nxt, MappingState.PROGRESS, nxt.time() / modifier.speedValue());
        }

        public void clear() {
            iterator.clear();
            started = ended = !iterator.hasNext();
        }
    }
}
