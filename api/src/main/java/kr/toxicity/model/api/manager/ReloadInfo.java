/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.manager;

import lombok.Builder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the context for a plugin reload operation.
 * <p>
 * This record holds information about who initiated the reload and whether certain parts of the reload should be skipped.
 * </p>
 *
 * @param skipConfig whether to skip reloading the main configuration file
 * @param sender the command sender who initiated the reload
 * @since 1.15.2
 */
@Builder
public record ReloadInfo(boolean skipConfig, @NotNull CommandSender sender) {
    /**
     * The default reload info, representing a standard reload initiated from the console.
     * @since 1.15.2
     */
    public static final ReloadInfo DEFAULT = ReloadInfo.builder()
        .skipConfig(false)
        .sender(Bukkit.getConsoleSender())
        .build();
}
