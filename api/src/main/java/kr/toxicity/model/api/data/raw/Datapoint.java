package kr.toxicity.model.api.data.raw;

import com.google.gson.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.lang.reflect.Type;
import java.util.function.Function;

/**
 * A raw JSON vector.
 * @param x x
 * @param y y
 * @param z z
 * @param script script
 */
@ApiStatus.Internal
public record Datapoint(
        float x,
        float y,
        float z,
        @Nullable String script
) {
    /**
     * Parser instance
     */
    public static final Parser PARSER = new Parser();

    /**
     * Parser
     */
    public static final class Parser implements Function<JsonElement, Datapoint>, JsonDeserializer<Datapoint> {

        private Parser() {
        }

        @Override
        public Datapoint deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return apply(json);
        }

        @Override
        public Datapoint apply(JsonElement element) {
            var array = element.getAsJsonObject();
            var script = array.getAsJsonPrimitive("script");
            return new Datapoint(
                    parse(array.getAsJsonPrimitive("x")),
                    parse(array.getAsJsonPrimitive("y")),
                    parse(array.getAsJsonPrimitive("z")),
                    script != null ? script.getAsString() : null
            );
        }

        private static float parse(@Nullable JsonPrimitive primitive) {
            if (primitive == null) return 0;
            try {
                var f = primitive.getAsFloat();
                return Float.isNaN(f) ? 0 : f;
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }


    /**
     * Converts to vector.
     * @return vector
     */
    public @NotNull Vector3f toVector() {
        return new Vector3f(x, y, z);
    }
}
