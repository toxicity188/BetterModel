/*
 * This source file is part of BetterModel.
 * Copyright (c) 2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.animation;

import kr.toxicity.model.api.tracker.Tracker;
import kr.toxicity.model.api.util.MathUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

import static kr.toxicity.model.api.util.CollectionUtil.newSequencedChainingMap;

/**
 * Animation state handler
 * @param <T> timed value
 */
@RequiredArgsConstructor
@ApiStatus.Internal
public final class AnimationStateHandler<T extends Timed> {

    private final T initialValue;
    private final BiConsumer<T, T> setConsumer;

    private final SequencedMap<String, TreeIterator> animators = newSequencedChainingMap();
    private final SequencedCollection<TreeIterator> reversedView = animators.sequencedValues().reversed();
    private final AtomicBoolean forceUpdateAnimation = new AtomicBoolean();

    @Getter
    private int delay;
    private volatile TreeIterator currentIterator = null;
    private volatile T beforeKeyframe = null, afterKeyframe = null;

    /**
     * Checks this keyframe has been finished
     * @return finished
     */
    public boolean keyframeFinished() {
        return delay <= 0;
    }

    /**
     * Gets before keyframe
     * @return before keyframe
     */
    public T beforeKeyframe() {
        return beforeKeyframe;
    }

    /**
     * Gets after keyframe
     * @return after keyframe
     */
    public T afterKeyframe() {
        return afterKeyframe;
    }

    /**
     * Gets before keyframe
     * @param defaultValue default value
     * @return before keyframe
     */
    @NotNull
    public T beforeKeyframe(@NotNull T defaultValue) {
        var value = beforeKeyframe;
        return value != null ? value : defaultValue;
    }

    /**
     * Gets after keyframe
     * @param defaultValue default value
     * @return after keyframe
     */
    @NotNull
    public T afterKeyframe(@NotNull T defaultValue) {
        var value = afterKeyframe;
        return value != null ? value : defaultValue;
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
        return frame == 0 ? 0 : Math.clamp((float) delay / frame, 0F, 1F);
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
                        return setAfterKeyframe(next.next());
                    }
                } else if (currentIterator != next) {
                    if (updateKeyframe(iterator, next)) {
                        currentIterator.clear();
                        currentIterator = next;
                        return setAfterKeyframe(next.next());
                    }
                } else if (keyframeFinished()) {
                    if (updateKeyframe(iterator, next)) {
                        return setAfterKeyframe(next.next());
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
            next.removeTask.run();
            iterator.remove();
            return false;
        } else {
            return true;
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
     * @param removeTask remove task
     */
    public void addAnimation(@NotNull String name, @NotNull AnimationIterator<T> iterator, @NotNull AnimationModifier modifier, @NotNull Runnable removeTask) {
        synchronized (animators) {
            animators.putLast(name, new TreeIterator(name, iterator, modifier, removeTask));
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
                .build(), v.removeTask));
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
        return afterKeyframe != null ? 20 * Tracker.MINECRAFT_TICK_MULTIPLIER * (currentIterator.time + MathUtil.FRAME_EPSILON) : 0F;
    }

    private class TreeIterator implements BooleanSupplier {
        private final RunningAnimation animation;
        private final AnimationIterator<T> iterator;
        private final AnimationModifier modifier;
        private final Runnable removeTask;

        private final T previous;

        private boolean started = false;
        private boolean ended = false;

        private float time = 0;

        public TreeIterator(String name, AnimationIterator<T> iterator, AnimationModifier modifier, Runnable removeTask) {
            animation = new RunningAnimation(name, iterator.type());
            this.iterator = iterator;
            this.modifier = modifier;
            this.removeTask = removeTask;

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
                time = (float) modifier.start() / 20;
                return iterator.next();
            }
            if (!iterator.hasNext()) {
                ended = true;
                time = (float) modifier.end() / 20;
                return previous;
            }
            var nxt = iterator.next();
            time = nxt.time() / modifier.speedValue();
            return nxt;
        }

        public void clear() {
            iterator.clear();
            started = ended = !iterator.hasNext();
        }
    }
}
