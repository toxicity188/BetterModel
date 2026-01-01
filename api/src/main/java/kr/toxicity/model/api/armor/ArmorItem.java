/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.armor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Armor item
 * @param tint tint value
 * @param type armor type
 * @param trim trim
 * @param palette palette
 */
public record ArmorItem(int tint, @NotNull String type, @Nullable String trim, @Nullable String palette) {
}
