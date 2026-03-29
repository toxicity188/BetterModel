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

public record Float2(
    float x,
    float y
) {

    public static final JsonDeserializer<Float2> PARSER = (json, _, _) -> {
        var array = json.getAsJsonArray();
        return new Float2(
            array.get(0).getAsFloat(),
            array.get(1).getAsFloat()
        );
    };

    public @NotNull Vector2f toVector() {
        return new Vector2f(x, y);
    }
}
