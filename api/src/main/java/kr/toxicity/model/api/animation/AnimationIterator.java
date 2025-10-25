/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.animation;

import com.google.gson.annotations.SerializedName;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;

/**
 * A keyframe iterator of animation.
 * @param <T> keyframe type
 */
public sealed interface AnimationIterator<T extends Timed> extends Iterator<T> {

    /**
     * Clears this iterator
     */
    void clear();

    /**
     * Gets an animation type
     * @return type
     */
    @NotNull Type type();

    /**
     * Animation type
     */
    @RequiredArgsConstructor
    enum Type {
        /**
         * Play once
         */
        @SerializedName("once")
        PLAY_ONCE {
            @Override
            public @NotNull <T extends Timed> AnimationIterator<T> create(@NotNull List<T> keyframes) {
                return new PlayOnce<>(keyframes);
            }
        },
        /**
         * Loop
         */
        @SerializedName("loop")
        LOOP {
            @Override
            public @NotNull <T extends Timed> AnimationIterator<T> create(@NotNull List<T> keyframes) {
                return new Loop<>(keyframes);
            }
        },
        /**
         * Hold on last
         */
        @SerializedName("hold")
        HOLD_ON_LAST {
            @Override
            public @NotNull <T extends Timed> AnimationIterator<T> create(@NotNull List<T> keyframes) {
                return new HoldOnLast<>(keyframes);
            }
        }
        ;

        /**
         * Creates iterator by given keyframes
         * @param keyframes keyframes
         * @return iterator
         * @param <T> keyframe type
         */
        public abstract <T extends Timed> @NotNull AnimationIterator<T> create(@NotNull List<T> keyframes);
    }

    /**
     * Play once
     * @param <T> keyframe time
     */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class PlayOnce<T extends Timed> implements AnimationIterator<T> {
        private final List<T> keyframe;
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
     * Hold on last
     * @param <T> keyframe time
     */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class HoldOnLast<T extends Timed> implements AnimationIterator<T> {
        private final List<T> keyframe;
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
     * Loop
     * @param <T> keyframe time
     */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class Loop<T extends Timed> implements AnimationIterator<T> {
        private final List<T> keyframe;
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