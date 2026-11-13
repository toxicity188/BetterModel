/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.animation.keyframe;

import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

/**
 * Applies evaluated keyframe components to a destination vector.
 * <pre>{@code
 * var result = KeyframeOperator.ADD.operate(1F, 2F, 3F, new Vector3f());
 * }</pre>
 *
 * @since 3.7.0
 */
@FunctionalInterface
public interface KeyframeOperator {

    /**
     * Adds the evaluated components to the destination.
     * @since 3.7.0
     */
    KeyframeOperator ADD = (x, y, z, dest) -> dest.add(x, y, z);
    /**
     * Multiplies each destination component by one plus the corresponding scale offset.
     * @since 3.7.0
     */
    KeyframeOperator SCALE_MUL = (x, y, z, dest) -> dest.mul(x + 1F, y + 1F, z + 1F);
    /**
     * Replaces the destination components with the evaluated values.
     * @since 3.7.0
     */
    KeyframeOperator SET = (x, y, z, dest) -> dest.set(x, y, z);

    /**
     * Applies this operation to three evaluated components and a destination.
     * <pre>{@code
     * var result = KeyframeOperator.SET.operate(1F, 2F, 3F, new Vector3f());
     * }</pre>
     *
     * @param x the X value
     * @param y the Y value
     * @param z the Z value
     * @param dest the destination vector
     * @return the vector produced by the operation
     * @throws RuntimeException if the destination operation fails
     * @since 3.7.0
     */
    @NotNull Vector3f operate(float x, float y, float z, @NotNull Vector3f dest);
}
