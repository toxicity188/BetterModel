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
 * Create event of fake tracker
 */
public record CreateDummyTrackerEvent(
    @NotNull DummyTracker tracker
) implements ModelEvent {
}
