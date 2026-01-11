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
 * Despawn tracker to some player
 */
public record ModelDespawnAtPlayerEvent(
    @NotNull PlatformPlayer player,
    @NotNull Tracker tracker
) implements ModelEvent {
}
