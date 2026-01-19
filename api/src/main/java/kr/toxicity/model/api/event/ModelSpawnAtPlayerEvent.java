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
 * Triggered when a model tracker is about to be spawned for a specific player.
 * <p>
 * This event allows preventing the model from spawning for that player.
 * </p>
 *
 * @since 2.0.0
 */
@Getter
@Setter
public final class ModelSpawnAtPlayerEvent implements CancellableEvent {

    private final Tracker tracker;
    private final PlatformPlayer player;
    private boolean cancelled;

    /**
     * Creates a new ModelSpawnAtPlayerEvent.
     *
     * @param player the player for whom the model is spawning
     * @param tracker the tracker being spawned
     * @since 2.0.0
     */
    @ApiStatus.Internal
    public ModelSpawnAtPlayerEvent(@NotNull PlatformPlayer player, @NotNull Tracker tracker) {
        this.tracker = tracker;
        this.player = player;
    }
}
