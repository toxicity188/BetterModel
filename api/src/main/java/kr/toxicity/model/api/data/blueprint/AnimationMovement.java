package kr.toxicity.model.api.data.blueprint;

import kr.toxicity.model.api.util.MathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

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
) implements Comparable<AnimationMovement> {

    @Override
    public int compareTo(@NotNull AnimationMovement o) {
        return Float.compare(time, o.time);
    }

    /**
     * Converts keyframe time.
     * @param newTime new time
     * @return new movement
     */
    public @NotNull AnimationMovement set(float newTime) {
        if (newTime == time || time == 0) return this;
        var mul = newTime / time;
        return new AnimationMovement(
                newTime,
                transform != null ? new Vector3f(transform)
                        .mul(mul) : null,
                scale != null ? new Vector3f(scale)
                        .mul(mul) : null,
                rotation != null ? new Vector3f(rotation)
                        .mul(mul) : null
        );
    }

    public @NotNull AnimationMovement lerpTick() {
        if (time % 0.05 == 0) return this;
        return set((float) Math.max(Math.round(time * 20F), 1) / 20F);
    }

    public @NotNull AnimationMovement lerp(@NotNull AnimationMovement to, float alpha) {
        if (alpha == 0) return this;
        else if (alpha == 1) return to;
        return new AnimationMovement(
                to.time,
                MathUtil.lerp(transform, to.transform, alpha),
                MathUtil.lerp(scale, to.scale, alpha),
                MathUtil.lerp(rotation, to.rotation, alpha)
        );
    }

    public @NotNull List<AnimationMovement> lerp(int lerpIndex, @NotNull AnimationMovement movement) {
        var list = new ArrayList<AnimationMovement>();
        var m = (1F / lerpIndex) * movement.time();
        var r = movement.time();
        for (int i = 0; i < lerpIndex; i++) {
            var t = (float) (i + 1) / (float) lerpIndex;
            var lerp = lerp(movement, t).time(Math.max(r % m, 0.05F)).lerpTick();
            list.add(lerp);
            r -= lerp.time();
        }
        return list;
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

    /**
     * Copy this movement to not null.
     * @return copied movement
     */
    public @NotNull AnimationMovement copyNotNull() {
        return new AnimationMovement(
                time,
                transform != null ? new Vector3f(transform) : new Vector3f(),
                scale != null ? new Vector3f(scale) : new Vector3f(),
                rotation != null ? new Vector3f(rotation) : new Vector3f()
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
