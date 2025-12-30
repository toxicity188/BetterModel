/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.tracker;

import com.google.gson.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

/**
 * Defines how a model's scale is calculated.
 * <p>
 * Scalers can be constant values, derived from entity attributes, or composites of multiple scalers.
 * They are serializable to JSON for configuration purposes.
 * </p>
 *
 * @since 1.15.2
 */
public sealed interface ModelScaler {

    /**
     * The global deserializer instance for scalers.
     * @since 1.15.2
     */
    Deserializer DESERIALIZER = new Deserializer();

    /**
     * Returns the name of this scaler type.
     *
     * @return the name
     * @since 1.15.2
     */
    @NotNull String name();

    /**
     * Calculates the scale for a given tracker.
     *
     * @param tracker the tracker
     * @return the calculated scale factor
     * @since 1.15.2
     */
    float scale(@NotNull Tracker tracker);

    /**
     * Returns the configuration data for this scaler as a JSON element.
     *
     * @return the data, or null if none
     * @since 1.15.2
     */
    @Nullable JsonElement data();

    /**
     * Deserializes a scaler from a JSON object.
     *
     * @param element the JSON object
     * @return the deserialized scaler, or the default scaler if invalid
     * @since 1.15.2
     */
    static @NotNull ModelScaler deserialize(@NotNull JsonObject element) {
        var scaler = DESERIALIZER.buildScaler(element);
        return scaler != null ? scaler : defaultScaler();
    }

    /**
     * Returns the default scaler (constant 1.0).
     *
     * @return the default scaler
     * @since 1.15.2
     */
    static @NotNull ModelScaler defaultScaler() {
        return DESERIALIZER.defaultScaler();
    }

    /**
     * Returns a scaler that uses the entity's scale attribute.
     *
     * @return the entity scaler
     * @since 1.15.2
     */
    static @NotNull ModelScaler entity() {
        return DESERIALIZER.entity.deserialize();
    }

    /**
     * Returns a constant value scaler.
     *
     * @param value the scale value
     * @return the value scaler
     * @since 1.15.2
     */
    static @NotNull ModelScaler value(float value) {
        return DESERIALIZER.value.deserialize(value);
    }

    /**
     * Creates a composite scaler that multiplies the results of multiple scalers.
     *
     * @param scalers the scalers to combine
     * @return the composite scaler
     * @since 1.15.2
     */
    static @NotNull ModelScaler composite(@NotNull ModelScaler... scalers) {
        return new Composite(new Composite.CompositeGetter(Arrays.asList(scalers)));
    }

    /**
     * Multiplies this scaler by a constant value.
     *
     * @param value the multiplier
     * @return the new composite scaler
     * @since 1.15.2
     */
    default @NotNull ModelScaler multiply(float value) {
        return composite(value(value));
    }

    /**
     * Multiplies this scaler by another scaler.
     *
     * @param scaler the other scaler
     * @return the new composite scaler
     * @since 1.15.2
     */
    default @NotNull ModelScaler composite(@NotNull ModelScaler scaler) {
        var list = new ArrayList<ModelScaler>();
        if (this instanceof Composite composite) {
            list.addAll(composite.getter.list);
        } else list.add(this);
        if (scaler instanceof Composite composite) {
            list.addAll(composite.getter.list);
        } else list.add(scaler);
        return new Composite(new Composite.CompositeGetter(list));
    }

    /**
     * Serializes this scaler to a JSON object.
     *
     * @return the JSON object
     * @since 1.15.2
     */
    default @NotNull JsonObject serialize() {
        var json = new JsonObject();
        json.addProperty("name", name());
        var d = data();
        if (d != null) json.add("data", d);
        return json;
    }

    /**
     * Functional interface for calculating scale.
     *
     * @since 1.15.2
     */
    interface Getter {
        /**
         * Default getter returning 1.0.
         * @since 1.15.2
         */
        Getter DEFAULT = t -> 1F;
        /**
         * Getter using entity scale.
         * @since 1.15.2
         */
        Getter ENTITY = t -> t instanceof EntityTracker entityTracker ? (float) entityTracker.registry().entity().scale() : 1F;

        /**
         * Calculates the scale.
         *
         * @param tracker the tracker
         * @return the scale
         * @since 1.15.2
         */
        float get(@NotNull Tracker tracker);

        /**
         * Creates a constant value getter.
         *
         * @param value the value
         * @return the getter
         * @since 1.15.2
         */
        static @NotNull Getter value(float value) {
            return t -> value;
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
         * @param data the JSON data
         * @return the getter, or null if invalid
         * @since 1.15.2
         */
        @Nullable Getter build(@NotNull JsonElement data);
    }

    /**
     * Implementation of a composite scaler.
     *
     * @since 1.15.2
     */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class Composite implements ModelScaler {

        private final CompositeGetter getter;

        private record CompositeGetter(@NotNull List<ModelScaler> list) implements Getter {
            @Override
            public float get(@NotNull Tracker tracker) {
                var f = 1F;
                for (ModelScaler modelScaler : list) {
                    f *= modelScaler.scale(tracker);
                }
                return f;
            }
        }

        @NotNull
        @Override
        public String name() {
            return "composite";
        }

        @Override
        public float scale(@NotNull Tracker tracker) {
            return getter.get(tracker);
        }

        private void add(@NotNull JsonArray array, @NotNull ModelScaler scaler) {
            if (scaler instanceof Composite composite) {
                for (ModelScaler childScaler : composite.getter.list) {
                    add(array, childScaler);
                }
            } else array.add(scaler.serialize());
        }

        @Override
        public JsonElement data() {
            var arr = new JsonArray();
            for (ModelScaler modelScaler : getter.list) {
                add(arr, modelScaler);
            }
            return arr.isEmpty() ? null : arr;
        }
    }

    /**
     * Helper interface for built-in deserializers.
     *
     * @since 1.15.2
     */
    interface BuiltInDeserializer extends Function<JsonElement, ModelScaler> {
        /**
         * Deserializes from a float value.
         *
         * @param value the value
         * @return the scaler
         * @since 1.15.2
         */
        default @NotNull ModelScaler deserialize(float value) {
            return apply(new JsonPrimitive(value));
        }
        /**
         * Deserializes a default instance.
         *
         * @return the scaler
         * @since 1.15.2
         */
        default @NotNull ModelScaler deserialize() {
            return apply(JsonNull.INSTANCE);
        }
    }

    /**
     * Registry and factory for scalers.
     *
     * @since 1.15.2
     */
    final class Deserializer {

        private final Map<String, Builder> getterMap = new HashMap<>();

        private final BuiltInDeserializer def = addScaler("default", d -> Getter.DEFAULT);
        private final BuiltInDeserializer entity = addScaler("entity", d -> Getter.ENTITY);
        private final BuiltInDeserializer value = addScaler("value", d -> d.isJsonPrimitive() ? Getter.value(d.getAsFloat()) : Getter.DEFAULT);

        private Deserializer() {
            getterMap.put("composite", d -> {
                if (d.isJsonArray()) {
                    return new Composite.CompositeGetter(d.getAsJsonArray()
                            .asList()
                            .stream()
                            .filter(JsonElement::isJsonObject)
                            .map(element -> buildScaler(element.getAsJsonObject()))
                            .filter(Objects::nonNull)
                            .toList());
                } else return Getter.DEFAULT;
            });
        }

        private @NotNull ModelScaler defaultScaler() {
            return def.deserialize();
        }

        /**
         * Registers a new scaler type.
         *
         * @param name the scaler name
         * @param builder the builder
         * @return a built-in deserializer helper
         * @since 1.15.2
         */
        public @NotNull BuiltInDeserializer addScaler(@NotNull String name, @NotNull Builder builder) {
            var put = getterMap.putIfAbsent(name, builder);
            var target = put != null ? put : builder;
            return element -> pack(name, target, element);
        }

        /**
         * Builds a scaler from a JSON object.
         *
         * @param rawData the JSON object
         * @return the scaler, or null if invalid
         * @since 1.15.2
         */
        public @Nullable ModelScaler buildScaler(@NotNull JsonObject rawData) {
            var n = rawData.getAsJsonPrimitive("name");
            if (n == null) return null;
            var name = n.getAsString();
            var get = getterMap.get(name);
            if (get == null) return null;
            var d = rawData.get("data");
            return pack(name, get, d);
        }

        private @NotNull ModelScaler pack(@NotNull String name, @NotNull Builder builder, @Nullable JsonElement data) {
            var build = Optional.ofNullable(builder.build(data != null ? data : JsonNull.INSTANCE))
                    .orElse(Getter.DEFAULT);
            return build instanceof Composite.CompositeGetter compositeGetter ? new Composite(compositeGetter) : new Pack(name, build, data);
        }

        private record Pack(@NotNull String name, @NotNull Getter getter, @Nullable JsonElement data) implements ModelScaler {
            @Override
            public float scale(@NotNull Tracker tracker) {
                return getter.get(tracker);
            }
        }
    }
}
