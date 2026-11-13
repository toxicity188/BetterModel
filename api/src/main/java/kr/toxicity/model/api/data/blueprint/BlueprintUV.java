/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.data.blueprint;

import com.google.gson.JsonObject;
import kr.toxicity.model.api.data.Float4;
import kr.toxicity.model.api.data.raw.ModelResolution;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

/**
 * Represents a processed face UV mapping blueprint ready for Minecraft model JSON serialization.
 *
 * @param faceId the directional face identifier (e.g., {@code "north"}, {@code "up"})
 * @param uv the raw UV bounds represented as {@link Float4}
 * @param rotation the rotation angle in degrees (0, 90, 180, 270)
 * @param texture the resolved index of the texture referenced by this face
 * @since 3.7.0
 */
public record BlueprintUV(
    @NotNull String faceId,
    @NotNull Float4 uv,
    int rotation,
    int texture
) {
    /**
     * The identifier string for the north face.
     * @since 3.7.0
     */
    public static final String NORTH = "north";
    /**
     * The identifier string for the south face.
     * @since 3.7.0
     */
    public static final String SOUTH = "south";
    /**
     * The identifier string for the east face.
     * @since 3.7.0
     */
    public static final String EAST = "east";
    /**
     * The identifier string for the west face.
     * @since 3.7.0
     */
    public static final String WEST = "west";
    /**
     * The identifier string for the up (top) face.
     * @since 3.7.0
     */
    public static final String UP = "up";
    /**
     * The identifier string for the down (bottom) face.
     * @since 3.7.0
     */
    public static final String DOWN = "down";

    /**
     * Serializes this UV mapping into a Minecraft model face {@link JsonObject}.
     * <p>
     * Scales and normalizes the UV coordinates based on the given {@link ModelResolution}.
     * If the normalized coordinates are invalid, returns {@code null}.
     * </p>
     *
     * @param resolution the texture resolution used to normalize UV coordinates
     * @return the serialized JSON representation of this face, or {@code null} if the UV is invalid
     * @since 3.7.0
     */
    public @Nullable JsonObject toJson(@NotNull ModelResolution resolution) {
        var div = uv.div(resolution);
        if (!div.isValid()) return null;
        var object = new JsonObject();
        object.add("uv", div.toJson());
        if (rotation != 0) object.addProperty("rotation", rotation);
        object.addProperty("tintindex", 0);
        object.addProperty("texture", "#" + texture);
        return object;
    }

    /**
     * Serializes a list of {@link BlueprintUV} instances into a combined JSON object representing
     * the "faces" section of a Minecraft element JSON.
     *
     * @param uvs the list of UV mappings to serialize
     * @param function the mapper of the texture resolution used to normalize UV coordinates
     * @return a {@link JsonObject} containing each face mapped to its serialized UV object,
     *         or {@code null} if all mappings are omitted or invalid
     * @since 3.7.0
     */
    public static @Nullable JsonObject toJson(@NotNull List<BlueprintUV> uvs, @NotNull Function<BlueprintUV, ModelResolution> function) {
        var object = new JsonObject();
        for (BlueprintUV uv : uvs) {
            var json = uv.toJson(function.apply(uv));
            if (json == null) continue;
            object.add(uv.faceId(), json);
        }
        return object.isEmpty() ? null : object;
    }
}
