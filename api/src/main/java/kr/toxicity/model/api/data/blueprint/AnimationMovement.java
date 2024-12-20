package kr.toxicity.model.api.data.blueprint;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public record AnimationMovement(
        long time,
        @Nullable Vector3f transform,
        @Nullable Vector3f scale,
        @Nullable Vector3f rotation
) implements Comparable<AnimationMovement> {

    private static final Vector3f ONE = new Vector3f();

    @Override
    public int compareTo(@NotNull AnimationMovement o) {
        return Long.compare(time, o.time);
    }

    public @NotNull AnimationMovement set(long newTime) {
        if (newTime == time) return this;
        if (newTime == 0 || time == 0) return this;
        return new AnimationMovement(
                newTime,
                transform != null ? new Vector3f(transform)
                        .mul(newTime)
                        .div(time) : null,
                scale != null ? new Vector3f(scale)
                        .sub(ONE)
                        .mul(newTime)
                        .div(time)
                        .add(ONE) : null,
                rotation != null ? new Vector3f(rotation)
                        .mul(newTime)
                        .div(time) : null
        );
    }

    public @NotNull AnimationMovement plus(@NotNull AnimationMovement other) {
        var min = Math.min(time, other.time);
        var get1 = set(min);
        var get2 = other.set(min);
        return new AnimationMovement(
                min,
                plus(get1.transform, get2.transform),
                mul(get1.scale, get2.scale),
                plus(get1.rotation, get2.rotation)
        );
    }

    private @Nullable Vector3f plus(@Nullable Vector3f one, @Nullable Vector3f two) {
        if (one != null && two != null) {
            return new Vector3f(one).add(two);
        } else if (one != null) return one;
        else return two;
    }
    private @Nullable Vector3f mul(@Nullable Vector3f one, @Nullable Vector3f two) {
        if (one != null && two != null) {
            return new Vector3f(one).mul(two);
        } else if (one != null) return one;
        else return two;
    }
}
