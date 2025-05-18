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
import java.util.function.Function;

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
        PLAY_ONCE(PlayOnce::new),
        /**
         * Loop
         */
        LOOP(Loop::new),
        /**
         * Hold on last
         */
        HOLD_ON_LAST(HoldOnLast::new)
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

        private final Function<List<AnimationMovement>, AnimationIterator> mapper;

        /**
         * Creates iterator by given keyframes
         * @param keyFrames keyframes
         * @return iterator
         */
        public @NotNull AnimationIterator create(@NotNull List<AnimationMovement> keyFrames) {
            return mapper.apply(keyFrames);
        }
    }

    /**
     * Play once
     */
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

    /**
     * Hold on last
     */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class HoldOnLast implements AnimationIterator {
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
            return index < keyFrame.size();
        }

        @Override
        @NotNull
        public AnimationMovement next() {
            return (index < keyFrame.size()) ? keyFrame.get(index++) : keyFrame.getLast();
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

    /**
     * Creates iterator per play once
     * @param keyFrame key frames
     * @return iterator
     */
    static @NotNull PlayOnce playOnce(@NotNull List<AnimationMovement> keyFrame) {
        return new PlayOnce(keyFrame);
    }

    /**
     * Creates iterator per loop
     * @param keyFrame key frames
     * @return iterator
     */
    static @NotNull Loop loop(@NotNull List<AnimationMovement> keyFrame) {
        return new Loop(keyFrame);
    }
}