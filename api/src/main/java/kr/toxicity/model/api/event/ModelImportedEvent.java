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
 * Model imported event
 */
public record ModelImportedEvent(
    @NotNull ModelBlueprint blueprint,
    @NotNull ModelRenderer renderer
) implements ModelEvent {
}
