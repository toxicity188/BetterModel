/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.animation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.joml.Vector3f;

import java.util.List;

/**
 * A movement of animation.
 * @param time keyframe time
 * @param position position
 * @param scale scale
 * @param rotation rotation
 * @param globalRotation global rotation
 * @param skipInterpolation skip interpolation
 */
public record AnimationMovement(
        float time,
        @Nullable Vector3f position,
        @Nullable Vector3f scale,
        @Nullable Vector3f rotation,
        boolean globalRotation,
        boolean skipInterpolation
) implements Timed {

    /**
     * Empty movement
     */
    public static final AnimationMovement EMPTY = new AnimationMovement(0);

    /**
     * Gets empty movement list
     * @param length length
     * @return empty keyframes
     */
    @NotNull
    @Unmodifiable
    public static List<AnimationMovement> withEmpty(float length) {
        return List.of(EMPTY, new AnimationMovement(length));
    }

    /**
     * Creates empty animation movement
     * @param time time
     */
    public AnimationMovement(float time) {
        this(time, null, null, null, false, false);
    }

    /**
     * Gets empty movement
     * @return empty movement
     */
    public @NotNull AnimationMovement empty() {
        if (!hasKeyframe()) return this;
        return time <= 0F ? EMPTY : new AnimationMovement(time);
    }

    /**
     * Checks this movement has some keyframe
     * @return has keyframe
     */
    public boolean hasKeyframe() {
        return position != null || scale != null || rotation != null;
    }
}
