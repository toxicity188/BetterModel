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
     * @param section yaml
     * @return config
     */
    public static @NotNull ModuleConfig from(@NotNull ConfigurationSection section) {
        return new ModuleConfig(
                section.getBoolean("model"),
                section.getBoolean("player-animation")
        );
    }
}
