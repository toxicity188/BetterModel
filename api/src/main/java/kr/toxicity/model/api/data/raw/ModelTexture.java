/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.raw;

import com.google.gson.annotations.SerializedName;
import kr.toxicity.model.api.data.blueprint.BlueprintTexture;
import kr.toxicity.model.api.util.PackUtil;
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
    public @NotNull BlueprintTexture toBlueprint(@NotNull ModelLoadContext context) {
        var name = nameWithoutExtension();
        return new BlueprintTexture(
            PackUtil.toPackName(name.startsWith("global_") ? name : context.name + "_" + name),
            Base64.getDecoder().decode(source().substring(source().indexOf(',') + 1)),
            width(),
            height(),
            uvWidth(),
            uvHeight(),
            !name.startsWith("-")
        );
    }

    /**
     * Gets texture's name without extension
     * @return name without extension
     */
    public @NotNull String nameWithoutExtension() {
        var name = name();
        var nameIndex = name.lastIndexOf('.');
        return nameIndex >= 0 ? name.substring(0, nameIndex) : name;
    }
}
