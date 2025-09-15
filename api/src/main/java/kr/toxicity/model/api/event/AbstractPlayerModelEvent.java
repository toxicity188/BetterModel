/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.event;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Player-related event
 */
@Getter
public abstract class AbstractPlayerModelEvent extends AbstractModelEvent {

    private final Player player;

    /**
     * Creates with player
     * @param player player
     */
    @ApiStatus.Internal
    public AbstractPlayerModelEvent(@NotNull Player player) {
        this.player = player;
    }
}
