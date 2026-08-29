/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.data;

import com.google.gson.JsonDeserializer;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;

/**
 * A simple record representing two float values.
 *
 * @param x the x value
 * @param y the y value
 * @since 3.0.0
 */
public record Float2(
    float x,
    float y
) {
    /**
     * A GSON deserializer for {@link Float2}.
     * @since 3.0.0
     */
    public static final JsonDeserializer<Float2> PARSER = (json, _, _) -> {
        var array = json.getAsJsonArray();
        return new Float2(
            array.get(0).getAsFloat(),
            array.get(1).getAsFloat()
        );
    };

    /**
     * Converts this record to a {@link Vector2f}.
     *
     * @return a new vector instance
     * @since 3.0.0
     */
    public @NotNull Vector2f toVector() {
        return new Vector2f(x, y);
    }
}
