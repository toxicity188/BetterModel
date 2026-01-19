/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.event;

import kr.toxicity.model.api.tracker.DummyTracker;
import org.jetbrains.annotations.NotNull;

/**
 * Triggered when a new {@link DummyTracker} is created.
 * <p>
 * This event allows plugins/mods to perform initialization or tracking logic for dummy trackers.
 * </p>
 *
 * @param tracker the newly created dummy tracker
 * @since 2.0.0
 */
public record CreateDummyTrackerEvent(
    @NotNull DummyTracker tracker
) implements ModelEvent {
}
