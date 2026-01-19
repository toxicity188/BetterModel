/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.manager;

import lombok.Builder;
import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the context for a platform reload operation.
 * <p>
 * This record holds information about who initiated the reload and whether certain parts of the reload should be skipped.
 * </p>
 *
 * @param skipConfig whether to skip reloading the main configuration file
 * @param sender the command sender who initiated the reload
 * @since 1.15.2
 */
@Builder
public record ReloadInfo(boolean skipConfig, @NotNull Audience sender) {
    /**
     * The default reload info, representing a standard reload initiated from the console.
     * @since 1.15.2
     */
    public static final ReloadInfo DEFAULT = ReloadInfo.builder()
        .skipConfig(false)
        .sender(Audience.empty())
        .build();
}
