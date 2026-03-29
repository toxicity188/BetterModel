/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.data.raw;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import kr.toxicity.model.api.data.Float4;
import kr.toxicity.model.api.data.blueprint.BlueprintLoadContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Represents the UV mapping data for a model face.
 * <p>
 * This record holds the UV coordinates, rotation, and texture index for a specific face of a model element.
 * </p>
 *
 * @param uv the UV coordinates as a {@link Float4} (u1, v1, u2, v2)
 * @param rotation the rotation of the UV map in degrees (0, 90, 180, 270)
 * @param texture the JSON element representing the texture index, can be null
 * @since 1.15.2
 */
public record ModelUV(
    @NotNull Float4 uv,
    int rotation,
    @Nullable JsonElement texture
) {

    /**
     * Checks if this UV mapping has a valid texture index.
     *
     * @return true if a texture is defined, false otherwise
     * @since 1.15.2
     */
    public boolean hasTexture() {
        return texture != null && texture.isJsonPrimitive() && texture.getAsJsonPrimitive().isNumber();
    }

    /**
     * Returns the texture index associated with this UV mapping.
     *
     * @return the texture index
     * @throws NullPointerException if no texture is defined
     * @since 1.15.2
     */
    public int textureIndex() {
        return Objects.requireNonNull(texture).getAsInt();
    }

    /**
     * Converts this UV data to a JSON object for the Minecraft model file.
     *
     * @param context the blueprint context, used for texture resolution
     * @param tint the tint index to apply
     * @return the generated JSON object
     * @since 1.15.2
     */
    public @NotNull JsonObject toJson(@NotNull BlueprintLoadContext context, int tint) {
        var object = new JsonObject();
        object.add("uv", uv.div(context.texture(textureIndex()).resolution(context.resolution())).toJson());
        if (rotation != 0) object.addProperty("rotation", rotation);
        object.addProperty("tintindex", tint);
        object.addProperty("texture", "#" + texture);
        return object;
    }
}
