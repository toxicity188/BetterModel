/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.animation.keyframe;

import com.google.gson.JsonPrimitive;
import kr.toxicity.model.api.BetterModel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

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

    @RequiredArgsConstructor
    final class Expression implements KeyframeDatapoint {

        private final String expression;
        private final Map<KeyframeCompileContext, Keyframe> memoized = new HashMap<>();

        @Override
        public @NotNull Keyframe compile(@NotNull KeyframeCompileContext context) {
            return memoized.computeIfAbsent(context, _ -> {
                var expr = expression;
                var time = Float.toString(context.animTime());
                expr = expr.replace("q.anim_time", time);
                expr = expr.replace("query.life_time", time);
                expr = expr.replace("q.anim_time", time);
                expr = expr.replace("query.life_time", time);
                return BetterModel.platform().evaluator().compile(expr);
            });
        }
    }
}
