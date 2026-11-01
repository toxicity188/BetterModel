/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.data.raw;

import kr.toxicity.model.api.data.blueprint.BlueprintFace;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import static kr.toxicity.model.api.data.blueprint.BlueprintUV.*;

/**
 * Represents the UV mappings for all six faces of a cube element.
 *
 * @param north the UV mapping for the north face
 * @param south the UV mapping for the south face
 * @param east the UV mapping for the east face
 * @param west the UV mapping for the west face
 * @param up the UV mapping for the up face
 * @param down the UV mapping for the down face
 * @since 1.15.2
 */
@ApiStatus.Internal
public record ModelFace(
    @NotNull ModelUV north,
    @NotNull ModelUV south,
    @NotNull ModelUV east,
    @NotNull ModelUV west,
    @NotNull ModelUV up,
    @NotNull ModelUV down
) {
    /**
     * Converts this raw face into a processed {@link BlueprintFace}.
     *
     * @return the blueprint face
     * @since 3.7.0
     */
    public @NotNull BlueprintFace toBlueprint() {
        return new BlueprintFace(
            north.hasTexture() ? north.toBlueprint(NORTH) : null,
            south.hasTexture() ? south.toBlueprint(SOUTH) : null,
            east.hasTexture() ? east.toBlueprint(EAST) : null,
            west.hasTexture() ? west.toBlueprint(WEST) : null,
            up.hasTexture() ? up.toBlueprint(UP) : null,
            down.hasTexture() ? down.toBlueprint(DOWN) : null
        );
    }
}
