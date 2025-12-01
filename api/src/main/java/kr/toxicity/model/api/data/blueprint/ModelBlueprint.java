/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
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
 * Parsed BlockBench model
 * @param name model name
 * @param resolution resolution
 * @param textures textures
 * @param group children group
 * @param animations animations
 */
@ApiStatus.Internal
public record ModelBlueprint(
    @NotNull String name,
    @NotNull ModelResolution resolution,
    @NotNull List<BlueprintTexture> textures,
    @NotNull List<BlueprintElement> group,
    @NotNull Map<String, BlueprintAnimation> animations
) {

    /**
     * Checks this blueprint has textures.
     * @return has textures
     */
    public boolean hasTexture() {
        return textures.stream().anyMatch(BlueprintTexture::canBeRendered);
    }

    /**
     * Builds blueprint image
     * @param obfuscator obfuscator
     * @return images
     */
    @NotNull
    @Unmodifiable
    public Stream<BlueprintImage> buildImage(@NotNull PackObfuscator obfuscator) {
        return textures.stream()
            .filter(BlueprintTexture::canBeRendered)
            .map(texture -> new BlueprintImage(
                texture.packName(obfuscator, name),
                texture.image(),
                texture.isAnimatedTexture() ? texture.toMcmeta() : null)
            );
    }
}
