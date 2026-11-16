/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.animation.keyframe;

import com.google.gson.JsonPrimitive;
import kr.toxicity.model.api.BetterModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An uncompiled scalar animation value represented by a number or a Molang expression.
 * <pre>{@code
 * var datapoint = KeyframeDatapoint.of(2F);
 * var keyframe = datapoint.compile(new KeyframeCompileContext(0F));
 * }</pre>
 *
 * @since 3.7.0
 */
public sealed interface KeyframeDatapoint {

    /**
     * The shared numeric zero datapoint.
     * @since 3.7.0
     */
    KeyframeDatapoint ZERO = of(0F);

    /**
     * Reads a numeric value or a Molang expression from a JSON primitive. Null and blank inputs yield zero.
     * The string transformation runs only for trimmed input that cannot be parsed as a float.
     * <pre>{@code
     * var datapoint = KeyframeDatapoint.fromJson(new JsonPrimitive("2.5"), text -> text);
     * }</pre>
     *
     * @param primitive the source primitive, or null
     * @param stringFunction the transformation applied to expression text
     * @return the numeric or expression datapoint
     * @throws RuntimeException if the string transformation throws
     * @since 3.7.0
     */
    static @NotNull KeyframeDatapoint fromJson(@Nullable JsonPrimitive primitive, @NotNull Function<String, String> stringFunction) {
        if (primitive == null) return ZERO;
        if (primitive.isNumber()) return of(primitive.getAsFloat());
        var string = primitive.getAsString().trim();
        if (string.isEmpty()) return ZERO;
        try {
            return of(Float.parseFloat(string));
        } catch (NumberFormatException _) {
            return of(stringFunction.apply(string));
        }
    }

    /**
     * Creates a numeric datapoint.
     * <pre>{@code
     * var datapoint = KeyframeDatapoint.of(2F);
     * }</pre>
     *
     * @param value the numeric value
     * @return the numeric datapoint
     * @since 3.7.0
     */
    static @NotNull KeyframeDatapoint of(float value) {
        return new Constant(value);
    }

    /**
     * Creates a Molang datapoint for later compilation.
     * <pre>{@code
     * var datapoint = KeyframeDatapoint.of("q.anim_time * 2");
     * }</pre>
     *
     * @param expression the Molang source expression
     * @return the expression datapoint
     * @since 3.7.0
     */
    static @NotNull KeyframeDatapoint of(@NotNull String expression) {
        return new Expression(expression);
    }

    /**
     * Compiles this datapoint into an evaluable scalar keyframe.
     * Molang compilation uses the active platform evaluator and may fail if the expression is invalid.
     * <pre>{@code
     * var keyframe = KeyframeDatapoint.of(2F).compile(new KeyframeCompileContext(0F));
     * }</pre>
     *
     * @param context the context used to resolve animation time
     * @return the compiled scalar keyframe
     * @throws RuntimeException if the evaluator fails during compilation
     * @since 3.7.0
     */
    @NotNull Keyframe compile(@NotNull KeyframeCompileContext context);

    /**
     * A numeric datapoint that compiles to a constant scalar keyframe.
     * <pre>{@code
     * var datapoint = new KeyframeDatapoint.Constant(2F);
     * }</pre>
     *
     * @param value the numeric source value
     * @since 3.7.0
     */
    record Constant(float value) implements KeyframeDatapoint {
        @Override
        public @NotNull Keyframe compile(@NotNull KeyframeCompileContext context) {
            return Keyframe.of(value);
        }
    }

    /**
     * A Molang expression used as a keyframe component. Compilation resolves {@code anim_time}
     * and {@code life_time} against the supplied animation time. Other queries pass to the evaluator.
     * <pre>{@code
     * var datapoint = KeyframeDatapoint.of("q.anim_time * 2");
     * var keyframe = datapoint.compile(new KeyframeCompileContext(0.5F));
     * }</pre>
     *
     * @param expression the Molang source expression
     * @since 3.7.0
     */
    record Expression(@NotNull String expression) implements KeyframeDatapoint {

        private static final Pattern QUERY_PATTERN = Pattern.compile("\\b(?:q|query)\\.([a-zA-Z_][a-zA-Z0-9_]*)\\b");
        private static final Map<String, Function<KeyframeCompileContext, Object>> QUERIES =
            Map.of(
                "anim_time", KeyframeCompileContext::animTime,
                "life_time", KeyframeCompileContext::animTime
            );

        @Override
        public @NotNull Keyframe compile(@NotNull KeyframeCompileContext context) {
            return BetterModel.platform().evaluator().compile(QUERY_PATTERN.matcher(expression).replaceAll(result -> {
                var query = QUERIES.get(result.group(1));
                return query != null ? Matcher.quoteReplacement(String.valueOf(query.apply(context))) : Matcher.quoteReplacement(result.group());
            }));
        }
    }
}
