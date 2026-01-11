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
 * An adapter of bukkit player
 */
public interface BaseBukkitPlayer extends BaseBukkitEntity, BasePlayer {
    @Override
    default @NotNull Player entity() {
        return ((BukkitPlayer) platform()).source();
    }
}
