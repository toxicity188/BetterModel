/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.event;

import kr.toxicity.model.api.tracker.EntityTracker;
import org.jetbrains.annotations.NotNull;

/**
 * Create event of entity tracker
 */
public record CreateEntityTrackerEvent(
    @NotNull EntityTracker tracker
) implements ModelEvent {
}
