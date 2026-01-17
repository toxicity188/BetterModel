/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.bukkit.platform;

import kr.toxicity.model.api.platform.PlatformOfflinePlayer;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record BukkitOfflinePlayer(@NotNull OfflinePlayer source) implements PlatformOfflinePlayer {
    @Override
    public @NotNull UUID uuid() {
        return source.getUniqueId();
    }

    @Override
    public @NotNull String name() {
        return source.getName();
    }
}
