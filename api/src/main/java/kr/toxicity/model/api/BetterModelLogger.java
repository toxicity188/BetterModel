/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * BetterModel's logger
 */
public interface BetterModelLogger {
    /**
     * Infos messages
     * @param message message
     */
    void info(@NotNull Component... message);

    /**
     * Warns message
     * @param message message
     */
    void warn(@NotNull Component... message);
}
