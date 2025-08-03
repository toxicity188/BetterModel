package kr.toxicity.model.api;

import kr.toxicity.model.api.util.function.Float2FloatFunction;
import org.jetbrains.annotations.NotNull;

/**
 * Evaluator
 */
public interface BetterModelEvaluator {
    /**
     * Compiles molang expression
     * @param expression expression
     * @return compiled function
     */
    @NotNull Float2FloatFunction compile(@NotNull String expression);
}
