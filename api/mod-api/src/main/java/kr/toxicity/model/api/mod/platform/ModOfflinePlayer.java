/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.mod.platform;

import kr.toxicity.model.api.platform.PlatformOfflinePlayer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Represents a Mod offline player wrapped as a {@link PlatformOfflinePlayer}.
 *
 * @param uuid the player UUID
 * @param name the player name, or null if unknown
 * @since 2.0.0
 */
public record ModOfflinePlayer(@NotNull UUID uuid, @Nullable String name) implements PlatformOfflinePlayer {
    @ApiStatus.Internal
    public ModOfflinePlayer {
    }

    /**
     * Creates a ModOfflinePlayer from the UUID and name.
     *
     * @param uuid the player uuid
     * @param name the player name
     * @return the instance
     * @since 2.0.0
     */
    public static @NotNull ModOfflinePlayer of(@NotNull UUID uuid, @Nullable String name) {
        return new ModOfflinePlayer(uuid, name);
    }
}
