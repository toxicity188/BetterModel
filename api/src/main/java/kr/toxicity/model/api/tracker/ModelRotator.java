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
 * Defines how a model's rotation is calculated and applied.
 * <p>
 * Rotators can modify the base rotation (e.g., only applying yaw, smoothing rotation)
 * and can be chained together.
 * </p>
 *
 * @since 1.15.2
 */
public sealed interface ModelRotator extends BiFunction<Tracker, ModelRotation, ModelRotation> {
    /**
     * The global deserializer instance for rotators.
     * @since 1.15.2
     */
    Deserializer DESERIALIZER = new Deserializer();
    /**
     * Default rotator (applies rotation as-is).
     * @since 1.15.2
     */
    @NotNull
    ModelRotator DEFAULT = Objects.requireNonNull(DESERIALIZER._default.apply());
    /**
     * Empty rotator (returns zero rotation).
     * @since 1.15.2
     */
    @NotNull
    ModelRotator EMPTY = Objects.requireNonNull(DESERIALIZER.empty.apply());
    /**
     * Pitch-only rotator.
     * @since 1.15.2
     */
    @NotNull
    ModelRotator PITCH = Objects.requireNonNull(DESERIALIZER.pitch.apply());
    /**
     * Yaw-only rotator.
     * @since 1.15.2
     */
    @NotNull
    ModelRotator YAW = Objects.requireNonNull(DESERIALIZER.yaw.apply());

    /**
     * Deserializes a rotator from a JSON object.
     *
     * @param object the JSON object
     * @return the deserialized rotator, or EMPTY if invalid
     * @since 1.15.2
     */
    static @NotNull ModelRotator deserialize(@NotNull JsonObject object) {
        var result = DESERIALIZER.deserialize(object);
        return result != null ? result : EMPTY;
    }

    /**
     * Creates a lazy rotator that smooths rotation over time.
     *
     * @param mills the smoothing duration in milliseconds
     * @return the lazy rotator
     * @since 1.15.2
     */
    static @NotNull ModelRotator lazy(long mills) {
        return Objects.requireNonNull(DESERIALIZER.lazy.apply(mills));
    }

    /**
     * Returns the name of this rotator type.
     *
     * @return the name
     * @since 1.15.2
     */
    @NotNull String name();

    /**
     * Returns the source rotator if this is a chained rotator.
     *
     * @return the source rotator, or null
     * @since 1.15.2
     */
    @Nullable ModelRotator source();

    /**
     * Returns the configuration data for this rotator.
     *
     * @return the data, or null
     * @since 1.15.2
     */
    @Nullable JsonElement data();

    /**
     * Returns the root rotator in the chain.
     *
     * @return the root rotator
     * @since 1.15.2
     */
    default @NotNull ModelRotator root() {
        var source = source();
        return source != null ? source.root() : this;
    }

    /**
     * Serializes this rotator to a JSON object.
     *
     * @return the JSON object
     * @since 1.15.2
     */
    default @NotNull JsonObject serialize() {
        var json = new JsonObject();
        json.addProperty("name", name());
        var d = data();
        if (d != null) json.add("data", d);
        var s = source();
        if (s != null) json.add("source", s.serialize());
        return json;
    }

    /**
     * Applies the rotator to a tracker with default rotation.
     *
     * @param tracker the tracker
     * @return the calculated rotation
     * @since 1.15.2
     */
    default @NotNull ModelRotation apply(@NotNull Tracker tracker) {
        return apply(tracker, ModelRotation.EMPTY);
    }

    /**
     * Applies the rotator to a tracker with a base rotation.
     *
     * @param tracker the tracker
     * @param rotation the base rotation
     * @return the calculated rotation
     * @since 1.15.2
     */
    @Override
    @NotNull
    ModelRotation apply(@NotNull Tracker tracker, @NotNull ModelRotation rotation);

    /**
     * Chains this rotator with another one.
     *
     * @param rotator the next rotator in the chain
     * @return the chained rotator
     * @since 1.15.2
     */
    default @NotNull ModelRotator then(@NotNull ModelRotator rotator) {
        return new SourcedRotator(this, rotator);
    }

    /**
     * Implementation of a chained rotator.
     *
     * @since 1.15.2
     */
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

    /**
     * Functional interface for calculating rotation.
     *
     * @since 1.15.2
     */
    interface Getter {
        /**
         * Default getter returning the input rotation.
         * @since 1.15.2
         */
        Getter DEFAULT = of(r -> r);
        /**
         * Calculates the rotation.
         *
         * @param tracker the tracker
         * @param modelRotation the base rotation
         * @return the calculated rotation
         * @since 1.15.2
         */
        @NotNull
        ModelRotation apply(@NotNull Tracker tracker, @NotNull ModelRotation modelRotation);

        /**
         * Creates a constant rotation getter.
         *
         * @param rotator the rotation
         * @return the getter
         * @since 1.15.2
         */
        static @NotNull Getter of(@NotNull ModelRotation rotator) {
            return (t, r) -> rotator;
        }
        /**
         * Creates a supplier-based rotation getter.
         *
         * @param rotator the supplier
         * @return the getter
         * @since 1.15.2
         */
        static @NotNull Getter of(@NotNull Supplier<ModelRotation> rotator) {
            return (t, r) -> rotator.get();
        }
        /**
         * Creates a function-based rotation getter.
         *
         * @param rotator the function
         * @return the getter
         * @since 1.15.2
         */
        static @NotNull Getter of(@NotNull Function<ModelRotation, ModelRotation> rotator) {
            return (t, r) -> rotator.apply(r);
        }
    }

    /**
     * Builder interface for creating Getters from JSON.
     *
     * @since 1.15.2
     */
    interface Builder {
        /**
         * Builds a getter from JSON data.
         *
         * @param element the JSON data
         * @return the getter, or null if invalid
         * @since 1.15.2
         */
        @Nullable Getter build(@NotNull JsonElement element);
    }

    /**
     * Helper interface for built-in deserializers.
     *
     * @since 1.15.2
     */
    interface BuiltInDeserializer extends Function<JsonElement, ModelRotator> {
        @Override
        @Nullable
        ModelRotator apply(@NotNull JsonElement element);

        /**
         * Deserializes a default instance.
         *
         * @return the rotator
         * @since 1.15.2
         */
        default @Nullable ModelRotator apply() {
            return apply(JsonNull.INSTANCE);
        }

        /**
         * Deserializes from a long value.
         *
         * @param value the value
         * @return the rotator
         * @since 1.15.2
         */
        default @Nullable ModelRotator apply(long value) {
            return apply(new JsonPrimitive(value));
        }
    }

    /**
     * Registry and factory for rotators.
     *
     * @since 1.15.2
     */
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

        /**
         * Registers a new rotator type.
         *
         * @param name the rotator name
         * @param builder the builder
         * @return a built-in deserializer helper
         * @since 1.15.2
         */
        public @NotNull BuiltInDeserializer register(@NotNull String name, @NotNull Builder builder) {
            var get = builderMap.putIfAbsent(name, builder);
            var selected = get != null ? get : builder;
            return e -> {
                var build = selected.build(e);
                var source = e.isJsonObject() ? e.getAsJsonObject().get("source") : null;
                return build != null ? pack(name, source != null && source.isJsonObject() ? deserialize(source.getAsJsonObject()) : null, e, build) : null;
            };
        }

        /**
         * Deserializes a rotator from a JSON object.
         *
         * @param object the JSON object
         * @return the rotator, or null if invalid
         * @since 1.15.2
         */
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
