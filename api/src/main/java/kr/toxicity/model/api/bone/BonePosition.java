/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.bone;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * Represents the position and state of a bone in a model.
 *
 * @param globalOffset the global offset vector
 * @param localOffset  the local offset vector
 * @param state        the unique identifier of the current state, or null if none
 * @since 2.0.2
 */
public record BonePosition(
    @NotNull Vector3f globalOffset,
    @NotNull Vector3f localOffset,
    @Nullable UUID state
) {
}
