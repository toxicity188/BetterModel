/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.nms;

import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Handles the transformation (position, scale, rotation) of a display entity.
 * <p>
 * This interface abstracts the interpolation logic for smooth animations.
 * </p>
 *
 * @since 1.15.2
 */
public interface DisplayTransformer {

    /**
     * Applies a transformation to the display.
     *
     * @param duration the interpolation duration in ticks
     * @param position the target position
     * @param scale the target scale
     * @param rotation the target rotation
     * @param bundler the packet bundler to use
     * @since 1.15.2
     */
    void transform(int duration, @NotNull Vector3f position, @NotNull Vector3f scale, @NotNull Quaternionf rotation, @NotNull PacketBundler bundler);

    /**
     * Sends the current transformation state to clients.
     *
     * @param bundler the packet bundler to use
     * @since 1.15.2
     */
    void sendTransformation(@NotNull PacketBundler bundler);
}
