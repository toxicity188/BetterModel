/*
 * This source file is part of BetterModel.
 * Copyright (c) 2025 toxicity188
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
 * @param playerArmor create player amor resource pack
 */
public record ModuleConfig(
    boolean model,
    boolean playerAnimation,
    boolean playerArmor
) {
    /**
     * Default config
     */
    public static final ModuleConfig DEFAULT = new ModuleConfig(
        true,
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
            predicate.test("player-animation"),
            predicate.test("player-armor")
        );
    }
}
