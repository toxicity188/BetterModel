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
 * A raw group of models.
 * @param name group name
 * @param uuid uuid
 * @param origin origin
 * @param rotation rotation
 * @param _visibility visibility
 */
public record ModelGroup(
    @NotNull String name,
    @NotNull String uuid,
    @Nullable Float3 origin,
    @Nullable Float3 rotation,
    @Nullable @SerializedName("visibility") Boolean _visibility
) {
    /**
     * Gets origin
     * @return origin
     */
    @Override
    @NotNull
    public Float3 origin() {
        return origin != null ? origin : Float3.ZERO;
    }

    /**
     * Gets rotation
     * @return rotation
     */
    @Override
    @NotNull
    public Float3 rotation() {
        return rotation != null ? rotation : Float3.ZERO;
    }

    /**
     * Gets visibility
     * @return visibility
     */
    public boolean visibility() {
        return !Boolean.FALSE.equals(_visibility);
    }
}
