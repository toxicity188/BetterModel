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
 * A bounding box with name.
 * @param name name
 * @param box box
 */
public record NamedBoundingBox(@NotNull BoneName name, @NotNull ModelBoundingBox box) {

    /**
     * Gets center location to vector.
     * @return vector
     */
    public @NotNull Vector3f centerPoint() {
        return new Vector3f().set(box.centerPoint());
    }

    /**
     * Gets centralized bounding box.
     * @return bounding box
     */
    public @NotNull ModelBoundingBox center() {
        return box.center();
    }
}
