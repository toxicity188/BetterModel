package kr.toxicity.model.api.data.raw;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.function.Function;

public record Float3(
        float x,
        float y,
        float z
) {
    public static final Float3 CENTER = new Float3(8, 8, 8);
    public static final Float3 PIVOT_POINT = new Float3(6, 16, 6);

    public static final Function<JsonElement, Float3> PARSER = element -> {
        if (element == null) return new Float3(0, 0, 0);
        var array = element.getAsJsonArray();
        return new Float3(
                array.get(0).getAsFloat(),
                array.get(1).getAsFloat(),
                array.get(2).getAsFloat()
        );
    };

    public float max() {
        return Math.max(x, Math.max(y, z));
    }
    public float min() {
        return Math.min(x, Math.min(y, z));
    }


    public @NotNull Float3 plus(@NotNull Float3 other) {
        return new Float3(
                x + other.x,
                y + other.y,
                z + other.z
        );
    }
    public @NotNull Float3 minus(@NotNull Float3 other) {
        return new Float3(
                x - other.x,
                y - other.y,
                z - other.z
        );
    }
    public @NotNull Float3 times(float value) {
        return new Float3(
                x * value,
                y * value,
                z * value
        );
    }
    public @NotNull Float3 div(float value) {
        return new Float3(
                x / value,
                y / value,
                z / value
        );
    }
    public @NotNull JsonArray toJson() {
        var array = new JsonArray();
        array.add(x);
        array.add(y);
        array.add(z);
        return array;
    }

    public @NotNull Vector3f toVector() {
        return new Vector3f(x, y, z);
    }
}
