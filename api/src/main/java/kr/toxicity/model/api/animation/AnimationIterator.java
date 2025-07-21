package kr.toxicity.model.api.animation;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;

/**
 * A keyframe iterator of animation.
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
        PLAY_ONCE {
            @Override
            public @NotNull <T extends Timed> AnimationIterator<T> create(@NotNull List<T> keyFrames) {
                return new PlayOnce<>(keyFrames);
            }
        },
        /**
         * Loop
         */
        LOOP {
            @Override
            public @NotNull <T extends Timed> AnimationIterator<T> create(@NotNull List<T> keyFrames) {
                return new Loop<>(keyFrames);
            }
        },
        /**
         * Hold on last
         */
        HOLD_ON_LAST {
            @Override
            public @NotNull <T extends Timed> AnimationIterator<T> create(@NotNull List<T> keyFrames) {
                return new HoldOnLast<>(keyFrames);
            }
        }
        ;

        /**
         * Deserializer
         */
        public static final JsonDeserializer<Type> DESERIALIZER = new JsonDeserializer<>() {
            @Override
            public Type deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                if (json.isJsonPrimitive()) {
                    return switch (json.getAsString()) {
                        case "loop" -> LOOP;
                        case "hold" -> HOLD_ON_LAST;
                        default -> PLAY_ONCE;
                    };
                }
                return PLAY_ONCE;
            }
        };
        /**
         * Creates iterator by given keyframes
         * @param keyFrames keyframes
         * @return iterator
         */
        public abstract <T extends Timed> @NotNull AnimationIterator<T> create(@NotNull List<T> keyFrames);
    }

    /**
     * Play once
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
     */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class HoldOnLast<T extends Timed> implements AnimationIterator<T> {
        private final List<T> keyFrame;
        private int index = 0;
        private boolean finished = false;

        @Override
        public void clear() {
            index = 0;
            finished = false;
        }

        @Override
        public boolean hasNext() {
            return true; // Fixed: Always returns true to keep the animation "alive".
        }

        @Override
        @NotNull
        public T next() {
            if (finished) {
                return keyFrame.getLast();
            }
            var nextFrame = keyFrame.get(index++);
            if (index >= keyFrame.size()) {
                finished = true; // Mark as finished entering the "hold" mode.
            }
            return nextFrame;
        }

        @NotNull
        @Override
        public Type type() {
            return Type.HOLD_ON_LAST;
        }
    }

    /**
     * Loop
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
            if (index >= keyFrame.size()) index = 1;
            return keyFrame.get(index++);
        }

        @NotNull
        @Override
        public Type type() {
            return Type.LOOP;
        }
    }
}