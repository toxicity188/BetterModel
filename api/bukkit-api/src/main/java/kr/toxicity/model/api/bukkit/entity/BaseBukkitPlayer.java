/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.bukkit.entity;

import kr.toxicity.model.api.bukkit.platform.BukkitPlayer;
import kr.toxicity.model.api.entity.BasePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a Bukkit-specific player adapter.
 * <p>
 * This interface extends {@link BaseBukkitEntity} and {@link BasePlayer} to provide
 * access to the underlying Bukkit player.
 * </p>
 *
 * @since 2.0.0
 */
public interface BaseBukkitPlayer extends BaseBukkitEntity, BasePlayer {
    /**
     * Returns the underlying Bukkit player.
     *
     * @return the Bukkit player
     * @since 2.0.0
     */
    @Override
    default @NotNull Player entity() {
        return ((BukkitPlayer) platform()).source();
    }
}
