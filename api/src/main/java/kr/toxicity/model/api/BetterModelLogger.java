package kr.toxicity.model.api;

import org.jetbrains.annotations.NotNull;

/**
 * BetterModel's logger
 */
public interface BetterModelLogger {
    /**
     * Infos messages
     * @param message message
     */
    void info(@NotNull String... message);

    /**
     * Warns message
     * @param message message
     */
    void warn(@NotNull String... message);
}
