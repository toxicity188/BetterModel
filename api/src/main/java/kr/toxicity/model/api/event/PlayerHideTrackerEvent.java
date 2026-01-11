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
 * Hides this tracker to some player
 */
@Getter
@Setter
public final class PlayerHideTrackerEvent implements CancellableEvent {

    private final Tracker tracker;
    private final PlatformPlayer player;
    private boolean cancelled;

    /**
     * Creates event
     * @param tracker tracker
     * @param player player
     */
    @ApiStatus.Internal
    public PlayerHideTrackerEvent(@NotNull Tracker tracker, @NotNull PlatformPlayer player) {
        this.tracker = tracker;
        this.player = player;
    }
}
