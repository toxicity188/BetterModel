/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.config;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

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
     * @param section yaml
     * @return config
     */
    public static @NotNull PackConfig from(@NotNull ConfigurationSection section) {
        return new PackConfig(
                section.getBoolean("generate-modern-model", true),
                section.getBoolean("generate-legacy-model", true),
                section.getBoolean("use-obfuscation", false)
        );
    }
}
