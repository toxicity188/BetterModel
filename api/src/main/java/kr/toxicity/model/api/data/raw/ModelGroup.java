/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.raw;

import com.google.gson.annotations.SerializedName;
import kr.toxicity.model.api.data.Float3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a group definition in the raw model data.
 * <p>
 * Groups are used to organize elements and other groups hierarchically.
 * This record corresponds to the group structure found in newer BlockBench versions (>= 5.0.0).
 * </p>
 *
 * @param name the name of the group
 * @param uuid the unique identifier of the group
 * @param origin the pivot point (origin) of the group
 * @param rotation the rotation of the group
 * @param _visibility the visibility state of the group (null means visible)
 * @since 1.15.2
 */
public record ModelGroup(
    @NotNull String name,
    @NotNull String uuid,
    @Nullable Float3 origin,
    @Nullable Float3 rotation,
    @Nullable @SerializedName("visibility") Boolean _visibility
) {
    /**
     * Returns the origin (pivot point) of the group.
     *
     * @return the origin, or {@link Float3#ZERO} if not specified
     * @since 1.15.2
     */
    @Override
    @NotNull
    public Float3 origin() {
        return origin != null ? origin : Float3.ZERO;
    }

    /**
     * Returns the rotation of the group.
     *
     * @return the rotation, or {@link Float3#ZERO} if not specified
     * @since 1.15.2
     */
    @Override
    @NotNull
    public Float3 rotation() {
        return rotation != null ? rotation : Float3.ZERO;
    }

    /**
     * Checks if the group is visible.
     *
     * @return true if visible, false otherwise
     * @since 1.15.2
     */
    public boolean visibility() {
        return !Boolean.FALSE.equals(_visibility);
    }
}
