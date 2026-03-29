/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.data.blueprint;

import kr.toxicity.model.api.data.raw.ModelResolution;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Represents a fully processed model blueprint, ready for generation and rendering.
 * <p>
 * This record contains all the necessary data derived from a raw model file, including
 * textures, structural elements (bones/cubes), and animations.
 * </p>
 *
 * @param name the name of the model
 * @param resolution the texture resolution of the model
 * @param textures the list of textures used by the model
 * @param elements the hierarchical list of model elements (bones)
 * @param animations a map of animations available for this model
 * @since 1.15.2
 */
@ApiStatus.Internal
public record ModelBlueprint(
    @NotNull String name,
    @NotNull ModelResolution resolution,
    @NotNull List<BlueprintTexture> textures,
    @NotNull List<BlueprintElement> elements,
    @NotNull Map<String, BlueprintAnimation> animations
) {

    public @NotNull BlueprintLoadContext context() {
        return new BlueprintLoadContext(
            name(),
            resolution(),
            textures()
        );
    }
}
