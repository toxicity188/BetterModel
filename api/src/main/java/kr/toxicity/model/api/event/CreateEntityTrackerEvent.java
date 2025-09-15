/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.event;

import kr.toxicity.model.api.tracker.EntityTracker;
import lombok.Getter;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Create event of entity tracker
 */
@Getter
public final class CreateEntityTrackerEvent extends CreateTrackerEvent {

    /**
     * Creates event
     * @param tracker tracker
     */
    @ApiStatus.Internal
    public CreateEntityTrackerEvent(@NotNull EntityTracker tracker) {
        super(tracker);
    }

    @NotNull
    public EntityTracker tracker() {
        return (EntityTracker) super.tracker();
    }

    /**
     * Gets source entity
     * @return source entity
     */
    @NotNull
    public Entity sourceEntity() {
        return tracker().sourceEntity();
    }
}
