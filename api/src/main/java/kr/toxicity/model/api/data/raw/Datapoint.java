package kr.toxicity.model.api.data.raw;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.function.Function;

/**
 * A raw json vector.
 * @param x x
 * @param y y
 * @param z z
 * @param script script
 */
public record Datapoint(
        float x,
        float y,
        float z,
        @Nullable String script
) {
    /**
     * Parser
     */
    public static final Function<JsonElement, Datapoint> PARSER = element -> {
        var array = element.getAsJsonObject();
        var script = array.getAsJsonPrimitive("script");
        return new Datapoint(
                parse(array.getAsJsonPrimitive("x")),
                parse(array.getAsJsonPrimitive("y")),
                parse(array.getAsJsonPrimitive("z")),
                script != null ? script.getAsString() : null
        );
    };

    private static float parse(@NotNull JsonPrimitive primitive) {
        var f = primitive.getAsFloat();
        return Float.isNaN(f) ? 0 : f;
    }

    /**
     * Converts to vector.
     * @return vector
     */
    public @NotNull Vector3f toVector() {
        return new Vector3f(x, y, z);
    }
}
