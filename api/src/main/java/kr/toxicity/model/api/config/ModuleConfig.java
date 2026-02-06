/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.config;

import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * Module config
 * @param model creates model
 * @param playerAnimation create player animation
 */
public record ModuleConfig(
    boolean model,
    boolean playerAnimation
) {
    /**
     * Default config
     */
    public static final ModuleConfig DEFAULT = new ModuleConfig(
        true,
        true
    );

    /**
     * Creates config from YAML
     * @param predicate predicate
     * @return config
     */
    public static @NotNull ModuleConfig from(@NotNull Predicate<String> predicate) {
        return new ModuleConfig(
            predicate.test("model"),
            predicate.test("player-animation")
        );
    }
}
