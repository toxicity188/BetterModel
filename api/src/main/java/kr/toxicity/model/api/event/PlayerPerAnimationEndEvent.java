/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.event;

import kr.toxicity.model.api.tracker.Tracker;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Ending per-player animation event
 */
@Getter
public final class PlayerPerAnimationEndEvent extends AbstractPlayerModelEvent {
    /**
     * Handler list
     */
    public static final HandlerList HANDLER_LIST = new HandlerList();

    private final Tracker tracker;

    /**
     * Creates event
     * @param tracker tracker
     * @param player player
     */
    @ApiStatus.Internal
    public PlayerPerAnimationEndEvent(@NotNull Tracker tracker, @NotNull Player player) {
        super(player);
        this.tracker = tracker;
    }

    /**
     * Gets tracker
     * @return tracker
     */
    public @NotNull Tracker tracker() {
        return tracker;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    /**
     * Gets a handler list
     * @return handler list
     */
    @SuppressWarnings("unused") //This method is necessary for event API.
    public static @NotNull HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
