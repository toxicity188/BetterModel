/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.nms;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Model nametag
 */
public interface ModelNametag {
    /**
     * Sets always visible flag
     * @param alwaysVisible always visible
     */
    void alwaysVisible(boolean alwaysVisible);

    /**
     * Sets component
     * @param component component
     */
    void component(@Nullable Component component);

    /**
     * Teleports this nametag
     * @param location location
     */
    void teleport(@NotNull Location location);

    /**
     * Sends nametag to some player
     * @param player player
     */
    void send(@NotNull Player player);

    /**
     * Removes this nametag
     * @param bundler bundler
     */
    void remove(@NotNull PacketBundler bundler);
}
