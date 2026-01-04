/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
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
 * Represents a raw texture definition from a model file.
 * <p>
 * This record contains the texture's metadata and its content encoded as a Base64 string.
 * </p>
 *
 * @param name the name of the texture file (e.g., "texture.png")
 * @param source the Base64-encoded content of the texture image
 * @param width the width of the texture in pixels
 * @param height the height of the texture in pixels
 * @param uvWidth the UV width of the texture
 * @param uvHeight the UV height of the texture
 * @since 1.15.2
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
     * Converts this raw texture into a processed {@link BlueprintTexture}.
     * <p>
     * This method decodes the Base64 source, generates a pack-compliant name, and determines if the texture should be included in the pack.
     * </p>
     *
     * @param context the model loading context
     * @return the blueprint texture
     * @since 1.15.2
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
     * Returns the texture name without its file extension.
     *
     * @return the name without extension
     * @since 1.15.2
     */
    public @NotNull String nameWithoutExtension() {
        var name = name();
        var nameIndex = name.lastIndexOf('.');
        return nameIndex >= 0 ? name.substring(0, nameIndex) : name;
    }
}
