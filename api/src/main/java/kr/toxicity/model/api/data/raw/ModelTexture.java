/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.raw;

import com.google.gson.annotations.SerializedName;
import kr.toxicity.model.api.data.blueprint.BlueprintTexture;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Base64;

/**
 * A raw model texture.
 * @param name texture's name
 * @param source texture's base64-encoded byte array
 * @param width width
 * @param height height
 * @param uvWidth uv-width
 * @param uvHeight uv-height
 */
@ApiStatus.Internal
public record ModelTexture(
        @NotNull String name,
        @NotNull String source,
        int width,
        int height,
        @SerializedName("uv_width") int uvWidth,
        @SerializedName("uv_height") int uvHeight
) {
    /**
     * Converts this texture to blueprint textures
     * @return converted textures
     */
    public @NotNull BlueprintTexture toBlueprint() {
        var nameIndex = name().indexOf('.');
        return new BlueprintTexture(
                nameIndex >= 0 ? name().substring(0, nameIndex) : name(),
                Base64.getDecoder().decode(source().substring(source().indexOf(',') + 1)),
                width(),
                height(),
                uvWidth(),
                uvHeight()
        );
    }
}
