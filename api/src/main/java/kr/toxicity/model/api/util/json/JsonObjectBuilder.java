/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.util.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * JSON object builder
 */
@ApiStatus.Internal
public final class JsonObjectBuilder {
    private final JsonObject object = new JsonObject();

    /**
     * Private initializer
     */
    private JsonObjectBuilder() {
    }

    /**
     * Creates builder
     * @return builder
     */
    public static @NotNull JsonObjectBuilder builder() {
        return new JsonObjectBuilder();
    }

    /**
     * Builds JSON object
     * @return build
     */
    public @NotNull JsonObject build() {
        return object;
    }

    /**
     * Adds JSON object
     * @param name name
     * @param consumer builder
     * @return self
     */
    public @NotNull JsonObjectBuilder jsonObject(@NotNull String name, @NotNull Consumer<JsonObjectBuilder> consumer) {
        var builder = builder();
        Objects.requireNonNull(consumer).accept(builder);
        if (builder.object.isEmpty()) return this;
        object.add(name, builder.build());
        return this;
    }

    /**
     * Adds JSON object
     * @param name name
     * @param jsonObject object
     * @return self
     */
    public @NotNull JsonObjectBuilder jsonObject(@NotNull String name, @Nullable JsonObject jsonObject) {
        if (jsonObject != null) object.add(name, jsonObject);
        return this;
    }

    /**
     * Adds JSON array
     * @param name name
     * @param array array
     * @return self
     */
    public @NotNull JsonObjectBuilder jsonArray(@NotNull String name, @Nullable JsonArray array) {
        if (array != null) object.add(name, array);
        return this;
    }

    /**
     * Adds JSON property
     * @param name name
     * @param property property
     * @return self
     */
    public @NotNull JsonObjectBuilder property(@NotNull String name, @NotNull String property) {
        object.addProperty(name, property);
        return this;
    }

    /**
     * Adds JSON property
     * @param name name
     * @param property property
     * @return self
     */
    public @NotNull JsonObjectBuilder property(@NotNull String name, @NotNull Boolean property) {
        object.addProperty(name, property);
        return this;
    }

    /**
     * Adds JSON property
     * @param name name
     * @param property property
     * @return self
     */
    public @NotNull JsonObjectBuilder property(@NotNull String name, @NotNull Number property) {
        object.addProperty(name, property);
        return this;
    }
}
