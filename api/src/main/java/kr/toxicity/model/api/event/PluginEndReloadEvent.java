/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.event;

import kr.toxicity.model.api.BetterModelPlatform;
import org.jetbrains.annotations.NotNull;

/**
 * Triggered when the BetterModel platform finishes reloading.
 * <p>
 * This event provides the result of the reload operation.
 * </p>
 *
 * @param result the result of the reload
 * @since 2.0.0
 */
public record PluginEndReloadEvent(
    @NotNull BetterModelPlatform.ReloadResult result
) implements ModelEvent {
}
