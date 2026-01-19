/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.event;

import kr.toxicity.model.api.platform.PlatformPlayer;
import kr.toxicity.model.api.tracker.Tracker;
import org.jetbrains.annotations.NotNull;

/**
 * Triggered when a model tracker is despawned for a specific player.
 * <p>
 * This event notifies plugins/mods that a model is no longer visible to a player.
 * </p>
 *
 * @param player the player for whom the model is despawning
 * @param tracker the tracker being despawned
 * @since 2.0.0
 */
public record ModelDespawnAtPlayerEvent(
    @NotNull PlatformPlayer player,
    @NotNull Tracker tracker
) implements ModelEvent {
}
