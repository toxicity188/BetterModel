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

@Builder
public record ReloadInfo(boolean firstReload, @NotNull CommandSender sender) {
    public static final ReloadInfo DEFAULT = ReloadInfo.builder()
            .firstReload(false)
            .sender(Bukkit.getConsoleSender())
            .build();
}
