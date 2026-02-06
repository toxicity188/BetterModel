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
 * Pack config
 * @param generateModernModel generate modern model
 * @param generateLegacyModel generate legacy model
 * @param useObfuscation use obfuscation
 */
public record PackConfig(
    boolean generateModernModel,
    boolean generateLegacyModel,
    boolean useObfuscation
) {
    /**
     * Default config
     */
    public static final PackConfig DEFAULT = new PackConfig(true, true, false);

    /**
     * Creates config from YAML
     * @param predicate predicate
     * @return config
     */
    public static @NotNull PackConfig from(@NotNull Predicate<String> predicate) {
        return new PackConfig(
            predicate.test("generate-modern-model"),
            predicate.test("generate-legacy-model"),
            predicate.test("use-obfuscation")
        );
    }
}
