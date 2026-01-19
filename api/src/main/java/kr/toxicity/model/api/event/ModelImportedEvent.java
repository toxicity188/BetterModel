/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.event;


import kr.toxicity.model.api.data.blueprint.ModelBlueprint;
import kr.toxicity.model.api.data.renderer.ModelRenderer;
import org.jetbrains.annotations.NotNull;

/**
 * Triggered when a model is successfully imported and registered.
 * <p>
 * This event provides access to the raw blueprint and the created renderer.
 * </p>
 *
 * @param blueprint the model blueprint
 * @param renderer the model renderer
 * @since 2.0.0
 */
public record ModelImportedEvent(
    @NotNull ModelBlueprint blueprint,
    @NotNull ModelRenderer renderer
) implements ModelEvent {
}
