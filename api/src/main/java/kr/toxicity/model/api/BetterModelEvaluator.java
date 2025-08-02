package kr.toxicity.model.api;

import org.jetbrains.annotations.NotNull;

/**
 * Evaluator
 */
public interface BetterModelEvaluator {
    /**
     * Evaluates molang expression
     * @param expression expression
     * @param time frame time
     * @return evaluated point
     */
    float evaluate(@NotNull String expression, float time);
}
