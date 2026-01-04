/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.raw;

import com.google.gson.JsonPrimitive;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.util.function.Float2FloatConstantFunction;
import kr.toxicity.model.api.util.function.Float2FloatFunction;
import kr.toxicity.model.api.util.function.FloatFunction;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Objects;

/**
 * Represents a single data point within a keyframe, which can be a static value or a Molang script.
 * <p>
 * This record holds the raw JSON values for x, y, and z coordinates, or a script string.
 * </p>
 *
 * @param x the x-coordinate value or script
 * @param y the y-coordinate value or script
 * @param z the z-coordinate value or script
 * @param script the script string (used for sound/particle effects)
 * @since 1.15.2
 */
@ApiStatus.Internal
public record ModelDatapoint(
    @Nullable JsonPrimitive x,
    @Nullable JsonPrimitive y,
    @Nullable JsonPrimitive z,
    @Nullable String script
) {

    /**
     * Checks if this data point contains a script.
     *
     * @return true if a script is present, false otherwise
     * @since 1.15.2
     */
    public boolean hasScript() {
        return script != null;
    }

    /**
     * Returns the script string.
     *
     * @return the script
     * @throws NullPointerException if no script is present
     * @since 1.15.2
     */
    @Override
    public @NotNull String script() {
        return Objects.requireNonNull(script);
    }

    /**
     * Converts this data point into a function that returns a {@link Vector3f}.
     * <p>
     * If the data point contains static values, it returns a constant function.
     * If it contains Molang expressions, it compiles them into a function that evaluates the expressions.
     * </p>
     *
     * @param context the model loading context
     * @return a function to get the vector value
     * @since 1.15.2
     */
    public @NotNull FloatFunction<Vector3f> toFunction(@NotNull ModelLoadContext context) {
        var xb = build(x, context);
        var yb = build(y, context);
        var zb = build(z, context);
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

    private static @NotNull Float2FloatFunction build(@Nullable JsonPrimitive primitive, @NotNull ModelLoadContext context) {
        if (primitive == null) return Float2FloatFunction.ZERO;
        if (primitive.isNumber()) return Float2FloatFunction.of(primitive.getAsFloat());
        var string = primitive.getAsString().trim();
        if (string.isEmpty()) return Float2FloatFunction.ZERO;
        try {
            return Float2FloatFunction.of(Float.parseFloat(string));
        } catch (NumberFormatException ignored) {
            return context.trySupply(
                () -> BetterModel.plugin().evaluator().compile(context.placeholder.parseVariable(string)),
                error -> new ModelLoadContext.Fallback<>(
                    Float2FloatFunction.ZERO,
                    "Cannot parse this datapoint: " + primitive + ", reason: " + error.getMessage()
                )
            );
        }
    }
}
