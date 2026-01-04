/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.blueprint;

import kr.toxicity.model.api.bone.BoneName;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

/**
 * Associates a {@link ModelBoundingBox} with a specific bone name.
 * <p>
 * This is used to identify which part of the model a hitbox corresponds to.
 * </p>
 *
 * @param name the name of the bone associated with this bounding box
 * @param box the bounding box definition
 * @since 1.15.2
 */
public record NamedBoundingBox(@NotNull BoneName name, @NotNull ModelBoundingBox box) {

    /**
     * Returns the center point of the bounding box as a {@link Vector3f}.
     *
     * @return the center vector
     * @since 1.15.2
     */
    public @NotNull Vector3f centerPoint() {
        return new Vector3f().set(box.centerPoint());
    }

    /**
     * Returns a new bounding box with the same dimensions but centered at the origin.
     *
     * @return the centered bounding box
     * @since 1.15.2
     */
    public @NotNull ModelBoundingBox center() {
        return box.center();
    }
}
