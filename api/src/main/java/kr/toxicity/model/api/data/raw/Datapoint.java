package kr.toxicity.model.api.data.raw;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.function.Function;

public record Datapoint(
        float x,
        float y,
        float z
) {
    public static final Function<JsonElement, Datapoint> PARSER = element -> {
        var array = element.getAsJsonObject();
        return new Datapoint(
                parse(array.getAsJsonPrimitive("x")),
                parse(array.getAsJsonPrimitive("y")),
                parse(array.getAsJsonPrimitive("z"))
        );
    };

    private static float parse(@NotNull JsonPrimitive primitive) {
        var f = primitive.getAsFloat();
        return Float.isNaN(f) ? 0 : f;
    }

    public @NotNull Vector3f toVector() {
        return new Vector3f(x, y, z);
    }
}
