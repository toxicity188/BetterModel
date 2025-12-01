/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.nms;

import kr.toxicity.model.api.tracker.EntityTrackerRegistry;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * A player channel
 */
public interface PlayerChannelHandler extends AutoCloseable, Identifiable, Profiled {

    /**
     * Gets Bukkit player
     * @return player
     */
    @NotNull Player player();

    /**
     * Sends correct entity data of this tracker
     * @param registry registry
     */
    void sendEntityData(@NotNull EntityTrackerRegistry registry);

    @Override
    void close();
}
