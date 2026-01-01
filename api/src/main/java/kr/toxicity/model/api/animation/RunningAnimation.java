/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.animation;

import org.jetbrains.annotations.NotNull;

/**
 * Running animation
 * @param name name
 * @param type type
 */
public record RunningAnimation(@NotNull String name, @NotNull AnimationIterator.Type type) {
}