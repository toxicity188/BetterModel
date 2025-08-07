package kr.toxicity.model.api.data.raw;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.util.function.Float2FloatConstantFunction;
import kr.toxicity.model.api.util.function.Float2FloatFunction;
import kr.toxicity.model.api.util.function.FloatFunction;
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

    private static @NotNull Float2FloatFunction build(@Nullable JsonPrimitive primitive, @NotNull ModelPlaceholder placeholder) {
        if (primitive == null) return Float2FloatFunction.ZERO;
        if (primitive.isNumber()) return Float2FloatFunction.of(primitive.getAsFloat());
        var string = primitive.getAsString().trim();
        if (string.isEmpty()) return Float2FloatFunction.ZERO;
        if (NUMBER_PATTERN.matcher(string).find()) return Float2FloatFunction.of(Float.parseFloat(string));
        return BetterModel.plugin().evaluator().compile(placeholder.parseVariable(string));
    }

    /**
     * Creates vector function
     * @param placeholder placeholder
     * @return vector function
     */
    public @NotNull FloatFunction<Vector3f> toFunction(@NotNull ModelPlaceholder placeholder) {
        var xb = build(x, placeholder);
        var yb = build(y, placeholder);
        var zb = build(z, placeholder);
        if (xb instanceof Float2FloatConstantFunction(float xc)
                && yb instanceof Float2FloatConstantFunction(float yc)
                && zb instanceof Float2FloatConstantFunction(float zc)
        ) {
            return FloatFunction.of(new Vector3f(xc, yc, zc));
        } else {
            return f -> new Vector3f(
                    xb.applyAsFloat(f),
                    yb.applyAsFloat(f),
                    zb.applyAsFloat(f)
            );
        }
    }
}
