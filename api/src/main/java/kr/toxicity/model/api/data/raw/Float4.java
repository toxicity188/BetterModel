package kr.toxicity.model.api.data.raw;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * A four float values (uv)
 * @param dx from-x
 * @param dz from-z
 * @param tx to-x
 * @param tz to-z
 */
@ApiStatus.Internal
public record Float4(
        float dx,
        float dz,
        float tx,
        float tz
) {
    /**
     * Parser
     */
    public static final Function<JsonElement, Float4> PARSER = element -> {
        var array = element.getAsJsonArray();
        return new Float4(
                array.get(0).getAsFloat(),
                array.get(1).getAsFloat(),
                array.get(2).getAsFloat(),
                array.get(3).getAsFloat()
        );
    };

    /**
     * Divides floats by resolution.
     * @param resolution model resolution
     * @return new floats
     */
    public @NotNull Float4 div(@NotNull ModelResolution resolution) {
        return div((float) resolution.width() / 16, (float) resolution.height() / 16);
    }

    /**
     * Divides floats by width, height
     * @param width width
     * @param height height
     * @return new floats
     */
    public @NotNull Float4 div(float width, float height) {
        return new Float4(
                dx / width,
                dz / height,
                tx / width,
                tz / height
        );
    }

    /**
     * Converts floats to json array.
     * @return json array
     */
    public @NotNull JsonArray toJson() {
        var array = new JsonArray();
        array.add(dx);
        array.add(dz);
        array.add(tx);
        array.add(tz);
        return array;
    }
}
