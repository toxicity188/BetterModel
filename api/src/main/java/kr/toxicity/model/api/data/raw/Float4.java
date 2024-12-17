package kr.toxicity.model.api.data.raw;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public record Float4(
        float dx,
        float dz,
        float tx,
        float tz
) {
    public static final Function<JsonElement, Float4> PARSER = element -> {
        var array = element.getAsJsonArray();
        return new Float4(
                array.get(0).getAsFloat(),
                array.get(1).getAsFloat(),
                array.get(2).getAsFloat(),
                array.get(3).getAsFloat()
        );
    };

    public @NotNull Float4 div(float value) {
        return new Float4(
                dx / value,
                dz / value,
                tx / value,
                tz / value
        );
    }

    public @NotNull JsonArray toJson() {
        var array = new JsonArray();
        array.add(dx);
        array.add(dz);
        array.add(tx);
        array.add(tz);
        return array;
    }
}
