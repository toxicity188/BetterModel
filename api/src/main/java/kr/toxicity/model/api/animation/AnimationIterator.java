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
public interface AnimationIterator<T extends Timed> extends Iterator<T> {

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
            public @NotNull <T extends Timed> AnimationIterator<T> create(@NotNull List<T> keyFrames) {
                return new PlayOnce<>(keyFrames);
            }
        },
        /**
         * Loop
         */
        @SerializedName("loop")
        LOOP {
            @Override
            public @NotNull <T extends Timed> AnimationIterator<T> create(@NotNull List<T> keyFrames) {
                return new Loop<>(keyFrames);
            }
        },
        /**
         * Hold on last
         */
        @SerializedName("hold")
        HOLD_ON_LAST {
            @Override
            public @NotNull <T extends Timed> AnimationIterator<T> create(@NotNull List<T> keyFrames) {
                return new HoldOnLast<>(keyFrames);
            }
        }
        ;

        /**
         * Creates iterator by given keyframes
         * @param keyFrames keyframes
         * @return iterator
         * @param <T> keyframe type
         */
        public abstract <T extends Timed> @NotNull AnimationIterator<T> create(@NotNull List<T> keyFrames);
    }

    /**
     * Play once
     * @param <T> keyframe time
     */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class PlayOnce<T extends Timed> implements AnimationIterator<T> {
        private final List<T> keyFrame;
        private int index = 0;

        @Override
        public void clear() {
            index = Integer.MAX_VALUE;
        }

        @Override
        public boolean hasNext() {
            return index < keyFrame.size();
        }

        @Override
        @NotNull
        public T next() {
            return keyFrame.get(index++);
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
        private final List<T> keyFrame;
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
            if (index >= keyFrame.size()) return keyFrame.getLast();
            return keyFrame.get(index++);
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
        private final List<T> keyFrame;
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
            if (index >= keyFrame.size()) index = 0;
            return keyFrame.get(index++);
        }

        @NotNull
        @Override
        public Type type() {
            return Type.LOOP;
        }
    }
}