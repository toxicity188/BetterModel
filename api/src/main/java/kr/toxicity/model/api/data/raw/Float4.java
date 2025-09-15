/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.raw;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializer;
import kr.toxicity.model.api.util.MathUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * A four float values (uv)
 * @param dx from-x
 * @param dz from-z
 * @param tx to-x
 * @param tz to-z
 */
@ApiStatus.Internal
public record Float4(
        float dx,
        float dz,
        float tx,
        float tz
) {

    /**
     * Parser
     */
    public static final JsonDeserializer<Float4> PARSER = (json, typeOfT, context) -> {
        var array = json.getAsJsonArray();
        return new Float4(
                array.get(0).getAsFloat(),
                array.get(1).getAsFloat(),
                array.get(2).getAsFloat(),
                array.get(3).getAsFloat()
        );
    };

    /**
     * Divides floats by resolution.
     * @param resolution model resolution
     * @return new floats
     */
    public @NotNull Float4 div(@NotNull ModelResolution resolution) {
        return div((float) resolution.width() / MathUtil.MODEL_TO_BLOCK_MULTIPLIER, (float) resolution.height() / MathUtil.MODEL_TO_BLOCK_MULTIPLIER);
    }

    /**
     * Divides floats by width, height
     * @param width width
     * @param height height
     * @return new floats
     */
    public @NotNull Float4 div(float width, float height) {
        return new Float4(
                dx / width,
                dz / height,
                tx / width,
                tz / height
        );
    }

    /**
     * Converts floats to JSON array.
     * @return json array
     */
    public @NotNull JsonArray toJson() {
        var array = new JsonArray(4);
        array.add(dx);
        array.add(dz);
        array.add(tx);
        array.add(tz);
        return array;
    }

    @Override
    public int hashCode() {
        var hash = 31;
        var value = 1;
        value = value * hash + MathUtil.similarHashCode(dx);
        value = value * hash + MathUtil.similarHashCode(dz);
        value = value * hash + MathUtil.similarHashCode(tx);
        value = value * hash + MathUtil.similarHashCode(tz);
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof Float4(float dx1, float dz1, float tx1, float tz1))) return false;
        return MathUtil.isSimilar(dx, dx1)
                && MathUtil.isSimilar(dz, dz1)
                && MathUtil.isSimilar(tx, tx1)
                && MathUtil.isSimilar(tz, tz1);
    }

    @Override
    public @NotNull String toString() {
        return toJson().toString();
    }
}
