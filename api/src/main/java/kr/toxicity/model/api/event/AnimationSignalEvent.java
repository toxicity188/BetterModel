/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.event;

import kr.toxicity.model.api.platform.PlatformPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Triggered when an animation script emits a signal.
 * <p>
 * This event allows plugins/mods to react to custom signals defined within BlockBench animations.
 * </p>
 *
 * @param player the player associated with the animation
 * @param signal the signal
 * @since 2.0.0
 */
public record AnimationSignalEvent(
    @NotNull PlatformPlayer player,
    @NotNull String signal
) implements ModelEvent {
}
