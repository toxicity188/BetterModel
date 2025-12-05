/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.nms;

import kr.toxicity.model.api.entity.BaseBukkitPlayer;
import kr.toxicity.model.api.tracker.EntityTrackerRegistry;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * A player channel
 */
public interface PlayerChannelHandler extends Identifiable, AutoCloseable {

    /**
     * Gets Bukkit player
     * @return player
     */
    default @NotNull Player player() {
        return base().entity();
    }

    @Override
    default @NotNull UUID uuid() {
        return base().uuid();
    }

    @Override
    default int id() {
        return base().id();
    }

    /**
     * Gets base player
     * @return base player
     */
    @NotNull BaseBukkitPlayer base();

    /**
     * Sends correct entity data of this tracker
     * @param registry registry
     */
    void sendEntityData(@NotNull EntityTrackerRegistry registry);

    @Override
    void close();
}
