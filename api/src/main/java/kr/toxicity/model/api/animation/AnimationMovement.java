package kr.toxicity.model.api.animation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * A movement of animation.
 * @param time keyframe time
 * @param transform position
 * @param scale scale
 * @param rotation rotation
 */
public record AnimationMovement(
        float time,
        @Nullable Vector3f transform,
        @Nullable Vector3f scale,
        @Nullable Vector3f rotation
) implements Timed {

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
        return new AnimationMovement(
                newTime,
                transform,
                scale,
                rotation
        );
    }

    /**
     * Adds other movement.
     * @param other other movement
     * @return new movement
     */
    public @NotNull AnimationMovement plus(@NotNull AnimationMovement other) {
        return new AnimationMovement(
                time + other.time,
                plus(transform, other.transform),
                plus(scale, other.scale),
                plus(rotation, other.rotation)
        );
    }

    /**
     * Subtracts other movement.
     * @param other other movement
     * @return new movement
     */
    public @NotNull AnimationMovement minus(@NotNull AnimationMovement other) {
        return new AnimationMovement(
                time - other.time(),
                minus(transform, other.transform),
                minus(scale, other.scale),
                minus(rotation, other.rotation)
        );
    }

    private @Nullable Vector3f plus(@Nullable Vector3f one, @Nullable Vector3f two) {
        if (one != null && two != null) {
            return new Vector3f(one).add(two);
        } else if (one != null) return one;
        else return two;
    }
    private @Nullable Vector3f minus(@Nullable Vector3f one, @Nullable Vector3f two) {
        if (one != null && two != null) {
            return new Vector3f(one).sub(two);
        } else if (one != null) return one;
        else return two;
    }
}
