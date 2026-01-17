/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.bukkit.platform;

import kr.toxicity.model.api.platform.PlatformWorld;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public record BukkitWorld(@NotNull World source) implements PlatformWorld {
}
