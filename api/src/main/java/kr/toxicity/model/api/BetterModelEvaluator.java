package kr.toxicity.model.api;

import org.jetbrains.annotations.NotNull;

public interface BetterModelEvaluator {
    float evaluate(@NotNull String expression, float time);
}
