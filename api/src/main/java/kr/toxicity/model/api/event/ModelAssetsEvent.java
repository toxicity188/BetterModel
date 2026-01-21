/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.event;

import kr.toxicity.model.api.data.ModelAsset;
import kr.toxicity.model.api.data.renderer.ModelRenderer;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Triggered when model assets are being gathered.
 * <p>
 * This event allows plugins to register custom {@link ModelAsset}s to be loaded by the engine.
 * </p>
 *
 * @param type the renderer type
 * @param assets the set of assets to be loaded
 * @since 2.0.0
 */
public record ModelAssetsEvent(@NotNull ModelRenderer.Type type, @NotNull Set<ModelAsset> assets) implements ModelEvent {
    /**
     * Adds a new model asset to the loading queue.
     *
     * @param asset the asset to add
     * @throws IllegalArgumentException if an asset with the same name already exists
     * @since 2.0.0
     */
    public void addAsset(@NotNull ModelAsset asset) {
        if (!assets.add(asset)) throw new IllegalArgumentException("Asset " + asset.name() + " already exists.");
    }
}
