/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.event;

import kr.toxicity.model.api.tracker.DummyTracker;
import lombok.Getter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Create event of fake tracker
 */
@Getter
public final class CreateDummyTrackerEvent extends CreateTrackerEvent {

    /**
     * Creates event
     * @param tracker tracker
     */
    @ApiStatus.Internal
    public CreateDummyTrackerEvent(@NotNull DummyTracker tracker) {
        super(tracker);
    }

    @NotNull
    public DummyTracker tracker() {
        return (DummyTracker) super.tracker();
    }
}
