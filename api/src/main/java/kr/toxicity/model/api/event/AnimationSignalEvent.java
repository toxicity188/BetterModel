/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Animation signal event
 */
public final class AnimationSignalEvent extends AbstractPlayerModelEvent {
    /**
     * Handler list
     */
    public static final HandlerList HANDLER_LIST = new HandlerList();

    private final String signal;

    /**
     * Creates signal event
     * @param player player
     * @param signal signal
     */
    @ApiStatus.Internal
    public AnimationSignalEvent(@NotNull Player player, @NotNull String signal) {
        super(player);
        this.signal = signal;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    /**
     * Gets signal
     * @return signal
     */
    public @NotNull String signal() {
        return signal;
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
