/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.util.json;

import com.google.gson.JsonArray;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Builds a JSON array containing non-empty JSON objects.
 *
 * <pre>{@code
 * var array = JsonArrayBuilder.builder()
 *     .jsonObject(item -> item.property("name", "example"))
 *     .build();
 * }</pre>
 */
public final class JsonArrayBuilder {

    private final JsonArray array = new JsonArray();

    /**
     * Creates builder
     * @return builder
     */
    public static @NotNull JsonArrayBuilder builder() {
        return new JsonArrayBuilder();
    }

    /**
     * Adds a JSON object if the consumer populates at least one property.
     *
     * @param consumer populates the object
     * @return this builder
     */
    @Contract("_ -> this")
    public @NotNull JsonArrayBuilder jsonObject(@NotNull Consumer<JsonObjectBuilder> consumer) {
        var builder = JsonObjectBuilder.builder();
        Objects.requireNonNull(consumer).accept(builder);
        var json = builder.build();
        if (!json.isEmpty()) array.add(json);
        return this;
    }

    /**
     * Returns the mutable array owned by this builder. Further additions affect the same array.
     *
     * @return the accumulated JSON array
     */
    @Contract(pure = true)
    public @NotNull JsonArray build() {
        return array;
    }
}
