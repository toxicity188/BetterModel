/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
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
 * Represents a nametag associated with a model part.
 * <p>
 * Nametags are typically implemented as invisible armor stands or text displays
 * that float above a specific bone.
 * </p>
 *
 * @since 1.15.2
 */
public interface ModelNametag {

    /**
     * Sets whether the nametag should always be visible (even through blocks).
     *
     * @param alwaysVisible true for always visible, false otherwise
     * @since 1.15.2
     */
    void alwaysVisible(boolean alwaysVisible);

    /**
     * Sets the text component of the nametag.
     *
     * @param component the text component, or null to clear
     * @since 1.15.2
     */
    void component(@Nullable Component component);

    /**
     * Teleports the nametag to a new location.
     *
     * @param location the target location
     * @since 1.15.2
     */
    void teleport(@NotNull Location location);

    /**
     * Sends the nametag packet to a specific player.
     *
     * @param player the target player
     * @since 1.15.2
     */
    void send(@NotNull Player player);

    /**
     * Removes the nametag.
     *
     * @param bundler the packet bundler to use
     * @since 1.15.2
     */
    void remove(@NotNull PacketBundler bundler);
}
