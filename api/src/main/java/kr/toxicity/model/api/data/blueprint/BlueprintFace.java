/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.data.blueprint;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Represents the 6-directional face UV mapping of a cube element in blueprint form.
 * <p>
 * This record holds processed UV mappings ready to be compiled into Minecraft model JSON faces.
 * </p>
 *
 * @param north the UV mapping for the north face, or {@code null} if omitted
 * @param south the UV mapping for the south face, or {@code null} if omitted
 * @param east the UV mapping for the east face, or {@code null} if omitted
 * @param west the UV mapping for the west face, or {@code null} if omitted
 * @param up the UV mapping for the up face, or {@code null} if omitted
 * @param down the UV mapping for the down face, or {@code null} if omitted
 * @since 3.7.0
 */
public record BlueprintFace(
    @Nullable BlueprintUV north,
    @Nullable BlueprintUV south,
    @Nullable BlueprintUV east,
    @Nullable BlueprintUV west,
    @Nullable BlueprintUV up,
    @Nullable BlueprintUV down
) {
    /**
     * An empty {@link BlueprintFace} instance with no face definitions.
     *
     * @since 3.7.0
     */
    public static final BlueprintFace EMPTY = new BlueprintFace(
        null,
        null,
        null,
        null,
        null,
        null
    );

    /**
     * Returns a sequential {@link Stream} containing all non-null {@link BlueprintUV}s defined on this face.
     *
     * @return a stream of valid UV mappings
     * @since 3.7.0
     */
    public @NotNull Stream<BlueprintUV> uv() {
        var builder = Stream.<BlueprintUV>builder();
        if (north != null) builder.add(north);
        if (south != null) builder.add(south);
        if (east != null) builder.add(east);
        if (west != null) builder.add(west);
        if (up != null) builder.add(up);
        if (down != null) builder.add(down);
        return builder.build();
    }

    /**
     * Returns an {@link IntStream} of all texture indices referenced by the defined faces.
     *
     * @return an int stream of texture indices
     * @since 3.7.0
     */
    public @NotNull IntStream textures() {
        return uv().mapToInt(BlueprintUV::texture);
    }
}
