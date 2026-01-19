/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.event;

import kr.toxicity.model.api.pack.PackZipper;
import org.jetbrains.annotations.NotNull;

/**
 * Triggered when the BetterModel platform starts reloading.
 * <p>
 * This event provides access to the {@link PackZipper}, allowing other plugins/mods to inject custom assets
 * into the resource pack before it is generated.
 * </p>
 *
 * @param zipper the pack zipper for adding assets
 * @since 2.0.0
 */
public record PluginStartReloadEvent(
    @NotNull PackZipper zipper
) implements ModelEvent {
}
