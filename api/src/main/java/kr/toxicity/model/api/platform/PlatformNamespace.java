/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.platform;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a namespaced key (e.g., "minecraft:apple").
 *
 * @param namespace the namespace (e.g., "minecraft")
 * @param path the path (e.g., "apple")
 * @since 2.0.0
 */
public record PlatformNamespace(@NotNull String namespace, @NotNull String path) {
}
