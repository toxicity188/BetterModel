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

public sealed interface KeyframeDatapoint {

    KeyframeDatapoint ZERO = of(0F);

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

    static @NotNull KeyframeDatapoint of(float value) {
        return new Constant(value);
    }

    static @NotNull KeyframeDatapoint of(@NotNull String expression) {
        return new Expression(expression);
    }

    @NotNull Keyframe compile(@NotNull KeyframeCompileContext context);

    record Constant(float value) implements KeyframeDatapoint {
        @Override
        public @NotNull Keyframe compile(@NotNull KeyframeCompileContext context) {
            return Keyframe.of(value);
        }
    }

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
