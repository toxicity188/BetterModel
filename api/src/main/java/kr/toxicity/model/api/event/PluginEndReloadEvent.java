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
 * Plugin reload end event
 */
public record PluginEndReloadEvent(
    @NotNull BetterModelPlatform.ReloadResult result
) implements ModelEvent {
}
