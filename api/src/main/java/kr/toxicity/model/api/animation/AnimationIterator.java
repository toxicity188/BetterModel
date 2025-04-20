package kr.toxicity.model.api.animation;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;

/**
 * A keyframe iterator of animation.
 */
public interface AnimationIterator extends Iterator<AnimationMovement> {
    /**
     * Gets the first element of animation.
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

    @NotNull Type type();

    enum Type {
        PLAY_ONCE,
        LOOP
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class PlayOnce implements AnimationIterator {
        private final List<AnimationMovement> keyFrame;
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
        @NotNull
        public AnimationMovement next() {
            return keyFrame.get(index++);
        }

        @NotNull
        @Override
        public Type type() {
            return Type.PLAY_ONCE;
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class Loop implements AnimationIterator {
        private final List<AnimationMovement> keyFrame;
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
        @NotNull
        public AnimationMovement next() {
            if (index >= keyFrame.size()) index = 0;
            return keyFrame.get(index++);
        }

        @NotNull
        @Override
        public Type type() {
            return Type.LOOP;
        }
    }

    static @NotNull PlayOnce playOnce(@NotNull List<AnimationMovement> keyFrame) {
        return new PlayOnce(keyFrame);
    }

    static @NotNull Loop loop(@NotNull List<AnimationMovement> keyFrame) {
        return new Loop(keyFrame);
    }
}