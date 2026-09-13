/*
 * This source file is part of BetterModel.
 * Copyright (c) 2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api;

import kr.toxicity.model.api.util.function.Float2FloatFunction;
import org.jetbrains.annotations.NotNull;

/**
 * Evaluator interface for parsing and compiling Molang expressions into executable functions.
 * <p>
 * This evaluator compiles string representations of Molang expressions used in model animations
 * into optimized float-to-float functions for fast runtime execution.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * BetterModelEvaluator evaluator = BetterModel.platform().evaluator();
 * Float2FloatFunction function = evaluator.compile("query.anim_time * 20");
 * float evaluated = function.apply(1.5f);
 * }</pre>
 */
public interface BetterModelEvaluator {
    /**
     * Compiles a Molang expression string into a high-performance evaluation function.
     *
     * @param expression the Molang expression string to compile (e.g., {@code "query.anim_time * 20"})
     * @return the compiled {@link Float2FloatFunction} that evaluates the expression at runtime
     */
    @NotNull Float2FloatFunction compile(@NotNull String expression);
}
