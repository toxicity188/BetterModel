/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.util.json;

import com.google.gson.JsonArray;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;

public final class JsonArrayBuilder {

    private final JsonArray array = new JsonArray();

    /**
     * Creates builder
     * @return builder
     */
    public static @NotNull JsonArrayBuilder builder() {
        return new JsonArrayBuilder();
    }

    public @NotNull JsonArrayBuilder jsonObject(@NotNull Consumer<JsonObjectBuilder> consumer) {
        var builder = JsonObjectBuilder.builder();
        Objects.requireNonNull(consumer).accept(builder);
        var json = builder.build();
        if (!json.isEmpty()) array.add(json);
        return this;
    }

    public @NotNull JsonArray build() {
        return array;
    }
}
