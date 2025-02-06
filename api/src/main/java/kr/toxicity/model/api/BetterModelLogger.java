package kr.toxicity.model.api;

import org.jetbrains.annotations.NotNull;

public interface BetterModelLogger {
    void info(@NotNull String... message);
    void warn(@NotNull String... message);
}
