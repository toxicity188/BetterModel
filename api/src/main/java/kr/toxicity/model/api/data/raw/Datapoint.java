package kr.toxicity.model.api.data.raw;

import com.google.gson.*;
import kr.toxicity.model.api.BetterModel;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.regex.Pattern;

/**
 * A raw JSON vector.
 * @param x x
 * @param y y
 * @param z z
 * @param script script
 */
@ApiStatus.Internal
public record Datapoint(
        @Nullable JsonPrimitive x,
        @Nullable JsonPrimitive y,
        @Nullable JsonPrimitive z,
        @Nullable String script
) {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("^(-?\\d+$|^-?\\d+\\.\\d+)$");

    /**
     * Parser instance
     */
    public static final JsonDeserializer<Datapoint> PARSER = (json, typeOfT, context) -> {
        var object = json.getAsJsonObject();
        var script = object.getAsJsonPrimitive("script");
        return new Datapoint(
                object.getAsJsonPrimitive("x"),
                object.getAsJsonPrimitive("y"),
                object.getAsJsonPrimitive("z"),
                script != null ? script.getAsString() : null
        );
    };

    private static float parse(@Nullable JsonPrimitive primitive, float time, @NotNull ModelPlaceholder placeholder) {
        if (primitive == null) return 0;
        if (primitive.isNumber()) return primitive.getAsFloat();
        var string = primitive.getAsString().trim();
        if (string.isEmpty()) return 0;
        if (NUMBER_PATTERN.matcher(string).find()) return Float.parseFloat(string);
        return BetterModel.plugin().evaluator().evaluate(placeholder.parseVariable(string), time);
    }

    /**
     * Converts to vector.
     * @return vector
     */
    public @NotNull Vector3f toVector(float time, @NotNull ModelPlaceholder placeholder) {
        return new Vector3f(
                parse(x, time, placeholder),
                parse(y, time, placeholder),
                parse(z, time, placeholder)
        );
    }
}
