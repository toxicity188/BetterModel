package kr.toxicity.model.api.animation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * A movement of animation.
 * @param time keyframe time
 * @param position position
 * @param scale scale
 * @param rotation rotation
 */
public record AnimationMovement(
        float time,
        @Nullable Vector3f position,
        @Nullable Vector3f scale,
        @Nullable Vector3f rotation
) implements Timed {

    /**
     * Empty movement
     */
    public static final AnimationMovement EMPTY = new AnimationMovement(0);

    /**
     * Creates empty animation movement
     * @param time time
     */
    public AnimationMovement(float time) {
        this(time, null, null, null);
    }

    /**
     * Sets keyframe time.
     * @param newTime new time
     * @return new movement
     */
    public @NotNull AnimationMovement time(float newTime) {
        if (time == newTime) return this;
        return new AnimationMovement(
                newTime,
                position,
                scale,
                rotation
        );
    }
}
