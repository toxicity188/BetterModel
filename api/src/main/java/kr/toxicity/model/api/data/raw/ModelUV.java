/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.raw;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import kr.toxicity.model.api.data.blueprint.ModelBlueprint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * UV data of a model
 * @param uv uv
 * @param rotation rotation
 * @param texture texture
 */
public record ModelUV(
    @NotNull Float4 uv,
    float rotation,
    @Nullable JsonElement texture
) {

    /**
     * Checks this UV has textures.
     * @return has texture
     */
    public boolean hasTexture() {
        return texture != null && texture.isJsonPrimitive() && texture.getAsJsonPrimitive().isNumber();
    }

    /**
     * Gets texture index
     * @return texture index
     */
    public int textureIndex() {
        return Objects.requireNonNull(texture).getAsInt();
    }

    /**
     * Gets json data of uv
     * @param parent parent blueprint
     * @param tint tint index
     * @return json
     */
    public @NotNull JsonObject toJson(@NotNull ModelBlueprint parent, int tint) {
        var object = new JsonObject();
        object.add("uv", uv.div(parent.textures().get(textureIndex()).resolution(parent.resolution())).toJson());
        if (rotation != 0) object.addProperty("rotation", rotation);
        object.addProperty("tintindex", tint);
        object.addProperty("texture", "#" + texture);
        return object;
    }
}
