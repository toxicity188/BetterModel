/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.data.raw;

import com.google.gson.JsonObject;
import kr.toxicity.model.api.data.blueprint.BlueprintLoadContext;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.stream.IntStream;

/**
 * Represents the UV mappings for all six faces of a cube element.
 *
 * @param north the UV mapping for the north face
 * @param east the UV mapping for the east face
 * @param south the UV mapping for the south face
 * @param west the UV mapping for the west face
 * @param up the UV mapping for the up face
 * @param down the UV mapping for the down face
 * @since 1.15.2
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
     * Converts the face UV data to a JSON object for the Minecraft model file.
     * <p>
     * Only faces with a defined texture will be included in the output.
     * </p>
     *
     * @param parent the parent model blueprint, used for texture resolution
     * @param tint the tint index to apply
     * @return the generated JSON object
     * @since 1.15.2
     */
    public @NotNull JsonObject toJson(@NotNull BlueprintLoadContext parent, int tint) {
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
     * Checks if any face has a texture defined.
     *
     * @return true if at least one face has a texture, false otherwise
     * @since 1.15.2
     */
    public boolean hasTexture() {
        return north.hasTexture()
            || east.hasTexture()
            || south.hasTexture()
            || west.hasTexture()
            || up.hasTexture()
            || down.hasTexture();
    }

    public @NotNull IntStream textureIndex() {
        var builder = IntStream.builder();
        if (north.hasTexture()) builder.add(north.textureIndex());
        if (east.hasTexture()) builder.add(east.textureIndex());
        if (south.hasTexture()) builder.add(south.textureIndex());
        if (west.hasTexture()) builder.add(west.textureIndex());
        if (up.hasTexture()) builder.add(up.textureIndex());
        if (down.hasTexture()) builder.add(down.textureIndex());
        return builder.build();
    }
}
