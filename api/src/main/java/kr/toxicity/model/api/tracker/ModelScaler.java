package kr.toxicity.model.api.tracker;

import com.google.gson.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

public sealed interface ModelScaler {

    Deserializer DESERIALIZER = new Deserializer();

    @NotNull String name();
    float scale(@NotNull Tracker tracker);
    @Nullable JsonElement data();

    static @NotNull ModelScaler deserialize(@NotNull JsonObject element) {
        var scaler = DESERIALIZER.buildScaler(element);
        return scaler != null ? scaler : defaultScaler();
    }

    static @NotNull ModelScaler defaultScaler() {
        return DESERIALIZER.defaultScaler();
    }

    static @NotNull ModelScaler entity() {
        return DESERIALIZER.entity.deserialize();
    }
    static @NotNull ModelScaler value(float value) {
        return DESERIALIZER.value.deserialize(value);
    }

    static @NotNull ModelScaler composite(@NotNull ModelScaler... scalers) {
        return new Composite(new Composite.CompositeGetter(Arrays.asList(scalers)));
    }

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

    default @NotNull JsonObject serialize() {
        var json = new JsonObject();
        json.addProperty("name", name());
        var d = data();
        if (d != null) json.add("data", d);
        return json;
    }

    interface Getter {
        Getter DEFAULT = t -> 1F;
        Getter ENTITY = t -> t instanceof EntityTracker entityTracker ? (float) entityTracker.registry().adapter().scale() : 1F;
        float get(@NotNull Tracker tracker);

        static Getter value(float value) {
            return t -> value;
        }
    }

    interface Builder {
        @Nullable Getter build(@NotNull JsonElement data);
    }

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

    interface BuiltInDeserializer extends Function<JsonElement, ModelScaler> {
        default @NotNull ModelScaler deserialize(float value) {
            return apply(new JsonPrimitive(value));
        }
        default @NotNull ModelScaler deserialize() {
            return apply(JsonNull.INSTANCE);
        }
    }

    final class Deserializer {

        private final Map<String, Builder> getterMap = new HashMap<>();

        private final BuiltInDeserializer def = addScaler("default", d -> Getter.DEFAULT);
        private final BuiltInDeserializer entity = addScaler("entity", d -> Getter.ENTITY);
        private final BuiltInDeserializer value = addScaler("value", d -> d.isJsonPrimitive() ? Getter.value(d.getAsFloat()) : Getter.DEFAULT);

        private Deserializer() {
            getterMap.put("composite", d -> {
                if (d.isJsonArray()) {
                    var list = new ArrayList<ModelScaler>();
                    for (JsonElement jsonElement : d.getAsJsonArray()) {
                        if (jsonElement.isJsonObject()) {
                            var child = buildScaler(jsonElement.getAsJsonObject());
                            if (child != null) list.add(child);
                        }
                    }
                    return new Composite.CompositeGetter(list);
                } else return Getter.DEFAULT;
            });
        }

        public @NotNull ModelScaler defaultScaler() {
            return def.deserialize();
        }

        public @NotNull BuiltInDeserializer addScaler(@NotNull String name, @NotNull Builder builder) {
            var put = getterMap.putIfAbsent(name, builder);
            var target = put != null ? put : builder;
            return element -> pack(name, target, element);
        }

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
