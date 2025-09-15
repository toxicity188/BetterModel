/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.tracker;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import kr.toxicity.model.api.util.lazy.LazyFloatProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Model rotator
 */
public sealed interface ModelRotator extends BiFunction<Tracker, ModelRotation, ModelRotation> {
    /**
     * Deserializer
     */
    Deserializer DESERIALIZER = new Deserializer();
    /**
     * Default rotator
     */
    @NotNull
    ModelRotator DEFAULT = Objects.requireNonNull(DESERIALIZER._default.apply());
    /**
     * Empty rotator
     */
    @NotNull
    ModelRotator EMPTY = Objects.requireNonNull(DESERIALIZER.empty.apply());
    /**
     * Pitch rotator
     */
    @NotNull
    ModelRotator PITCH = Objects.requireNonNull(DESERIALIZER.pitch.apply());
    /**
     * Yaw rotator
     */
    @NotNull
    ModelRotator YAW = Objects.requireNonNull(DESERIALIZER.yaw.apply());

    static @NotNull ModelRotator deserialize(@NotNull JsonObject object) {
        var result = DESERIALIZER.deserialize(object);
        return result != null ? result : EMPTY;
    }

    static @NotNull ModelRotator lazy(long mills) {
        return Objects.requireNonNull(DESERIALIZER.lazy.apply(mills));
    }

    @NotNull String name();

    @Nullable ModelRotator source();

    @Nullable JsonElement data();

    default @NotNull ModelRotator root() {
        var source = source();
        return source != null ? source.root() : this;
    }

    default @NotNull JsonObject serialize() {
        var json = new JsonObject();
        json.addProperty("name", name());
        var d = data();
        if (d != null) json.add("data", d);
        var s = source();
        if (s != null) json.add("source", s.serialize());
        return json;
    }

    default @NotNull ModelRotation apply(@NotNull Tracker tracker) {
        return apply(tracker, ModelRotation.EMPTY);
    }

    @Override
    @NotNull
    ModelRotation apply(@NotNull Tracker tracker, @NotNull ModelRotation rotation);

    default @NotNull ModelRotator then(@NotNull ModelRotator rotator) {
        return new SourcedRotator(this, rotator);
    }

    record SourcedRotator(@NotNull ModelRotator source, @NotNull ModelRotator delegate) implements ModelRotator {
        @Override
        public @NotNull String name() {
            return delegate.name();
        }

        @Override
        public @Nullable JsonElement data() {
            return delegate.data();
        }

        @Override
        public @NotNull ModelRotation apply(@NotNull Tracker tracker, @NotNull ModelRotation rotation) {
            return delegate.apply(tracker, source.apply(tracker, rotation));
        }
    }

    interface Getter {
        Getter DEFAULT = of(r -> r);
        @NotNull
        ModelRotation apply(@NotNull Tracker tracker, @NotNull ModelRotation modelRotation);

        static @NotNull Getter of(@NotNull ModelRotation rotator) {
            return (t, r) -> rotator;
        }
        static @NotNull Getter of(@NotNull Supplier<ModelRotation> rotator) {
            return (t, r) -> rotator.get();
        }
        static @NotNull Getter of(@NotNull Function<ModelRotation, ModelRotation> rotator) {
            return (t, r) -> rotator.apply(r);
        }
    }

    interface Builder {
        @Nullable Getter build(@NotNull JsonElement element);
    }

    interface BuiltInDeserializer extends Function<JsonElement, ModelRotator> {
        @Override
        @Nullable
        ModelRotator apply(@NotNull JsonElement element);

        default @Nullable ModelRotator apply() {
            return apply(JsonNull.INSTANCE);
        }

        default @Nullable ModelRotator apply(long value) {
            return apply(new JsonPrimitive(value));
        }
    }

    final class Deserializer {
        private final Map<String, Builder> builderMap = new HashMap<>();

        private final BuiltInDeserializer _default = register("default", j -> Getter.of(r -> r));
        private final BuiltInDeserializer empty = register("empty", j -> Getter.of(ModelRotation.EMPTY));
        private final BuiltInDeserializer yaw = register("yaw", j -> Getter.of(ModelRotation::yaw));
        private final BuiltInDeserializer pitch = register("pitch", j -> Getter.of(ModelRotation::pitch));
        private final BuiltInDeserializer lazy = register("lazy", j -> {
            if (j.isJsonPrimitive()) {
                var f = j.getAsLong();
                var xLazy = new LazyFloatProvider(f);
                var yLazy = new LazyFloatProvider(f);
                return Getter.of(r -> new ModelRotation(xLazy.updateAndGet(r.x()), yLazy.updateAndGet(r.y())));
            } else return null;
        });

        private Deserializer() {
        }

        public @NotNull BuiltInDeserializer register(@NotNull String name, @NotNull Builder builder) {
            var get = builderMap.putIfAbsent(name, builder);
            var selected = get != null ? get : builder;
            return e -> {
                var build = selected.build(e);
                var source = e.isJsonObject() ? e.getAsJsonObject().get("source") : null;
                return build != null ? pack(name, source != null && source.isJsonObject() ? deserialize(source.getAsJsonObject()) : null, e, build) : null;
            };
        }

        public @Nullable ModelRotator deserialize(@NotNull JsonObject object) {
            var rawName = object.getAsJsonPrimitive("name");
            if (rawName == null) return null;
            var name = rawName.getAsString();
            var get = builderMap.get(name);
            if (get == null) return null;
            var data = object.get("data");
            var source = object.getAsJsonObject().get("source");
            var build = get.build(data == null ? JsonNull.INSTANCE : data);
            return build != null ? pack(
                    name,
                    source != null && source.isJsonObject() ? deserialize(source.getAsJsonObject()) : null,
                    data,
                    build
            ) : null;
        }

        private @NotNull Pack pack(@NotNull String name, @Nullable ModelRotator source, @Nullable JsonElement data, @NotNull Getter getter) {
            return new Pack(name, source, data, getter);
        }

        private record Pack(@NotNull String name, @Nullable ModelRotator source, @Nullable JsonElement data, @NotNull Getter delegate) implements ModelRotator {

            @Override
            public @NotNull ModelRotation apply(@NotNull Tracker tracker, @NotNull ModelRotation modelRotation) {
                return delegate.apply(tracker, modelRotation);
            }
        }
    }
}
