/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.raw;

import com.google.gson.JsonObject;
import kr.toxicity.model.api.data.blueprint.ModelBlueprint;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * A UV mappings of an element (cube).
 * @param north north
 * @param east east
 * @param south south
 * @param west west
 * @param up up
 * @param down down
 */
@ApiStatus.Internal
public record ModelFace(
    @NotNull ModelUV north,
    @NotNull ModelUV east,
    @NotNull ModelUV south,
    @NotNull ModelUV west,
    @NotNull ModelUV up,
    @NotNull ModelUV down
) {
    /**
     * Converts UV to JSON object.
     * @param parent parent
     * @param tint tint index
     * @return json object.
     */
    public @NotNull JsonObject toJson(@NotNull ModelBlueprint parent, int tint) {
        var object = new JsonObject();
        if (north.hasTexture()) object.add("north", north.toJson(parent, tint));
        if (east.hasTexture()) object.add("east", east.toJson(parent, tint));
        if (south.hasTexture()) object.add("south", south.toJson(parent, tint));
        if (west.hasTexture()) object.add("west", west.toJson(parent, tint));
        if (up.hasTexture()) object.add("up", up.toJson(parent, tint));
        if (down.hasTexture()) object.add("down", down.toJson(parent, tint));
        return object;
    }

    /**
     * Returns whether this UV has textures.
     * @return Whether this UV has textures.
     */
    public boolean hasTexture() {
        return north.hasTexture()
            || east.hasTexture()
            || south.hasTexture()
            || west.hasTexture()
            || up.hasTexture()
            || down.hasTexture();
    }
}
