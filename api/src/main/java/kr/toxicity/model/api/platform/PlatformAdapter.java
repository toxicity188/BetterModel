/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.platform;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Adapts platform-specific objects and operations to the BetterModel API.
 * <p>
 * This interface provides methods for retrieving players, creating items, and checking server state,
 * abstracting away the differences between platforms like Bukkit and Fabric.
 * </p>
 *
 * @since 2.0.0
 */
public interface PlatformAdapter {

    /**
     * Returns the server's default view distance.
     *
     * @return the view distance in chunks
     * @since 2.0.0
     */
    int serverViewDistance();

    /**
     * Checks if the current thread is the main server tick thread.
     *
     * @return true if on the tick thread, false otherwise
     * @since 2.0.0
     */
    boolean isTickThread();

    /**
     * Checks if it is safe to access region data from the current thread.
     *
     * @return true if safe, false otherwise
     * @since 2.0.0
     */
    boolean isRegionSafe();

    /**
     * Retrieves an online player by their UUID.
     *
     * @param uuid the player's UUID
     * @return the player, or null if not found/offline
     * @since 2.0.0
     */
    @Nullable PlatformPlayer player(@NotNull UUID uuid);

    /**
     * Retrieves an offline player by their UUID.
     *
     * @param uuid the player's UUID
     * @return the offline player
     * @since 2.0.0
     */
    @NotNull PlatformOfflinePlayer offlinePlayer(@NotNull UUID uuid);

    /**
     * Returns a platform-specific representation of an empty item stack (air).
     *
     * @return the air item stack
     * @since 2.0.0
     */
    @NotNull PlatformItemStack air();

    /**
     * Returns a location at coordinates (0, 0, 0) in a default or null world.
     *
     * @return the zero location
     * @since 2.0.0
     */
    @NotNull PlatformLocation zero();
}
