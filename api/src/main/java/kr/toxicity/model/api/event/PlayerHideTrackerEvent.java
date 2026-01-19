/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.event;

import kr.toxicity.model.api.platform.PlatformPlayer;
import kr.toxicity.model.api.tracker.Tracker;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Triggered when a tracker is about to be hidden from a specific player.
 * <p>
 * This event allows preventing the tracker from being hidden.
 * </p>
 *
 * @since 2.0.0
 */
@Getter
@Setter
public final class PlayerHideTrackerEvent implements CancellableEvent {

    private final Tracker tracker;
    private final PlatformPlayer player;
    private boolean cancelled;

    /**
     * Creates a new PlayerHideTrackerEvent.
     *
     * @param tracker the tracker being hidden
     * @param player the player from whom the tracker is being hidden
     * @since 2.0.0
     */
    @ApiStatus.Internal
    public PlayerHideTrackerEvent(@NotNull Tracker tracker, @NotNull PlatformPlayer player) {
        this.tracker = tracker;
        this.player = player;
    }
}
