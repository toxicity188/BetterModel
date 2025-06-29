package kr.toxicity.model.api.util.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

public final class JsonObjectBuilder {
    private final JsonObject object = new JsonObject();

    private JsonObjectBuilder() {
    }

    public static @NotNull JsonObjectBuilder builder() {
        return new JsonObjectBuilder();
    }

    public @NotNull JsonObject build() {
        return object;
    }

    public @NotNull JsonObjectBuilder jsonObject(@NotNull String name, @NotNull Consumer<JsonObjectBuilder> consumer) {
        var builder = builder();
        Objects.requireNonNull(consumer).accept(builder);
        object.add(name, builder.build());
        return this;
    }

    public @NotNull JsonObjectBuilder jsonObject(@NotNull String name, @Nullable JsonObject jsonObject) {
        if (jsonObject != null) object.add(name, jsonObject);
        return this;
    }

    public @NotNull JsonObjectBuilder jsonArray(@NotNull String name, @Nullable JsonArray array) {
        if (array != null) object.add(name, array);
        return this;
    }

    public @NotNull JsonObjectBuilder property(@NotNull String name, @NotNull String property) {
        object.addProperty(name, property);
        return this;
    }

    public @NotNull JsonObjectBuilder property(@NotNull String name, @NotNull Boolean property) {
        object.addProperty(name, property);
        return this;
    }

    public @NotNull JsonObjectBuilder property(@NotNull String name, @NotNull Number property) {
        object.addProperty(name, property);
        return this;
    }
}
