/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.blueprint;

import kr.toxicity.model.api.data.raw.ModelResolution;
import kr.toxicity.model.api.pack.PackObfuscator;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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

    /**
     * Checks if this blueprint contains any renderable textures.
     *
     * @return true if at least one texture is renderable, false otherwise
     * @since 1.15.2
     */
    public boolean hasTexture() {
        return textures.stream().anyMatch(BlueprintTexture::canBeRendered);
    }

    /**
     * Generates a stream of {@link BlueprintImage} objects for resource pack generation.
     *
     * @param obfuscator the obfuscator to use for texture names
     * @return a stream of blueprint images
     * @since 1.15.2
     */
    @NotNull
    @Unmodifiable
    public Stream<BlueprintImage> buildImage(@NotNull PackObfuscator obfuscator) {
        return textures.stream()
            .filter(BlueprintTexture::canBeRendered)
            .map(texture -> new BlueprintImage(
                texture.packName(obfuscator),
                texture.image(),
                texture.isAnimatedTexture() ? texture.toMcmeta() : null)
            );
    }
}
