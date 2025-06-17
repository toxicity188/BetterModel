package kr.toxicity.model.test;

import org.jetbrains.annotations.NotNull;

public interface ModelTester {
    void start(@NotNull BetterModelTest test);
    void end(@NotNull BetterModelTest test);
}
