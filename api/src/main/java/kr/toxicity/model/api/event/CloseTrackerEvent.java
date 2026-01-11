/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.event;

import kr.toxicity.model.api.tracker.Tracker;
import org.jetbrains.annotations.NotNull;

/**
 * Closing tracker event
 */
public record CloseTrackerEvent(
    @NotNull Tracker tracker,
    @NotNull Tracker.CloseReason reason
) implements ModelEvent {
}
