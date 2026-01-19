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
 * Triggered when a tracker is about to be shown to a specific player.
 * <p>
 * This event allows preventing the tracker from being shown.
 * </p>
 *
 * @since 2.0.0
 */
@Getter
@Setter
public final class PlayerShowTrackerEvent implements CancellableEvent {

    private final Tracker tracker;
    private final PlatformPlayer player;

    private boolean cancelled;

    /**
     * Creates a new PlayerShowTrackerEvent.
     *
     * @param tracker the tracker being shown
     * @param player the player to whom the tracker is being shown
     * @since 2.0.0
     */
    @ApiStatus.Internal
    public PlayerShowTrackerEvent(@NotNull Tracker tracker, @NotNull PlatformPlayer player) {
        this.tracker = tracker;
        this.player = player;
    }

    /**
     * Returns the tracker being shown.
     *
     * @return the tracker
     * @since 2.0.0
     */
    public @NotNull Tracker tracker() {
        return tracker;
    }
}
