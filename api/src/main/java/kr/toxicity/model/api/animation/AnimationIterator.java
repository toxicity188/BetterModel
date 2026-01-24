/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.animation;

import com.google.gson.annotations.SerializedName;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

/**
 * An iterator for traversing animation keyframes.
 * <p>
 * This interface supports different looping modes (play once, loop, hold on last)
 * and allows resetting the iteration state.
 * </p>
 *
 * @param <T> the type of keyframe (must implement {@link Timed})
 * @since 1.15.2
 */
public sealed interface AnimationIterator<T extends Timed> extends Iterator<T> {

    /**
     * Resets the iterator to its initial state.
     * @since 1.15.2
     */
    void clear();

    /**
     * Returns the type of this animation iterator.
     *
     * @return the animation type
     * @since 1.15.2
     */
    @NotNull Type type();

    /**
     * Defines the behavior of the animation iterator.
     * @since 1.15.2
     */
    @RequiredArgsConstructor
    enum Type {
        /**
         * Plays the animation once and then stops.
         * @since 1.15.2
         */
        @SerializedName("once")
        PLAY_ONCE {
            @Override
            public @NotNull <T extends Timed> AnimationIterator<T> create(@NotNull TimedStorage<T> keyframes) {
                return new PlayOnce<>(keyframes);
            }
        },
        /**
         * Loops the animation continuously.
         * @since 1.15.2
         */
        @SerializedName("loop")
        LOOP {
            @Override
            public @NotNull <T extends Timed> AnimationIterator<T> create(@NotNull TimedStorage<T> keyframes) {
                return new Loop<>(keyframes);
            }
        },
        /**
         * Plays the animation once and holds the last frame.
         * @since 1.15.2
         */
        @SerializedName("hold")
        HOLD_ON_LAST {
            @Override
            public @NotNull <T extends Timed> AnimationIterator<T> create(@NotNull TimedStorage<T> keyframes) {
                return new HoldOnLast<>(keyframes);
            }
        }
        ;

        /**
         * Creates a new iterator for the given keyframes based on this type.
         *
         * @param keyframes the keyframes to iterate over
         * @param <T> the type of keyframe
         * @return a new animation iterator
         * @since 1.15.2
         */
        public abstract <T extends Timed> @NotNull AnimationIterator<T> create(@NotNull TimedStorage<T> keyframes);
    }

    /**
     * Implementation for {@link Type#PLAY_ONCE}.
     *
     * @param <T> the type of keyframe
     * @since 1.15.2
     */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class PlayOnce<T extends Timed> implements AnimationIterator<T> {
        private final TimedStorage<T> keyframe;
        private int index = 0;

        @Override
        public void clear() {
            index = Integer.MAX_VALUE;
        }

        @Override
        public boolean hasNext() {
            return index < keyframe.size();
        }

        @Override
        @NotNull
        public T next() {
            return keyframe.get(index++);
        }

        @NotNull
        @Override
        public Type type() {
            return Type.PLAY_ONCE;
        }
    }

    /**
     * Implementation for {@link Type#HOLD_ON_LAST}.
     *
     * @param <T> the type of keyframe
     * @since 1.15.2
     */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class HoldOnLast<T extends Timed> implements AnimationIterator<T> {
        private final TimedStorage<T> keyframe;
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
        @NotNull
        public T next() {
            if (index >= keyframe.size()) return keyframe.getLast();
            return keyframe.get(index++);
        }

        @NotNull
        @Override
        public Type type() {
            return Type.HOLD_ON_LAST;
        }
    }

    /**
     * Implementation for {@link Type#LOOP}.
     *
     * @param <T> the type of keyframe
     * @since 1.15.2
     */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class Loop<T extends Timed> implements AnimationIterator<T> {
        private final TimedStorage<T> keyframe;
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
        @NotNull
        public T next() {
            if (index >= keyframe.size()) index = 0;
            return keyframe.get(index++);
        }

        @NotNull
        @Override
        public Type type() {
            return Type.LOOP;
        }
    }
}
