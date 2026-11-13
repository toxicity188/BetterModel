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
 * Pack config
 * @param useObfuscation use obfuscation
 */
public record PackConfig(
    boolean useObfuscation
) {
    /**
     * Default config
     */
    public static final PackConfig DEFAULT = new PackConfig(true);

    /**
     * Creates config from YAML
     * @param predicate predicate
     * @return config
     */
    public static @NotNull PackConfig from(@NotNull Predicate<String> predicate) {
        return new PackConfig(
            predicate.test("use-obfuscation")
        );
    }
}
