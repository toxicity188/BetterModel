/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.raw;

import com.google.gson.JsonObject;
import kr.toxicity.model.api.data.blueprint.ModelBlueprint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * UV data of a model
 * @param uv uv
 * @param rotation rotation
 * @param texture texture
 */
public record ModelUV(
        @NotNull Float4 uv,
        float rotation,
        @Nullable String texture
) {
    /**
     * Gets json data of uv
     * @param parent parent blueprint
     * @param tint tint index
     * @return json
     */
    public @NotNull JsonObject toJson(@NotNull ModelBlueprint parent, int tint) {
        var object = new JsonObject();
        if (texture == null) return object;
        int textureIndex = Integer.parseInt(texture);
        object.add("uv", uv.div(parent.resolution()
                .then(parent.textures().get(textureIndex).resolution())).toJson());
        if (rotation != 0) object.addProperty("rotation", rotation);
        object.addProperty("tintindex", tint);
        object.addProperty("texture", "#" + textureIndex);
        return object;
    }
}
