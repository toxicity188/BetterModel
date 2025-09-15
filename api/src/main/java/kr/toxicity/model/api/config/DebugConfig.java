/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.config;

import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Debug config
 * @param options options
 */
public record DebugConfig(@NotNull Set<DebugOption> options) {
    /**
     * Debug option
     */
    @RequiredArgsConstructor
    public enum DebugOption {
        /**
         * Debug stack trace of exception
         */
        EXCEPTION("exception"),
        /**
         * Debug hit-box entity
         */
        HITBOX("hitbox"),
        /**
         * Debug packing resource pack
         */
        PACK("pack"),
        /**
         * Debug tracker thread
         */
        TRACKER("tracker")
        ;
        private final String config;
    }

    /**
     * Checks this config has this option
     * @param option option
     * @return has or not
     */
    public boolean has(@NotNull DebugOption option) {
        return options.contains(option);
    }

    /**
     * Default config
     */
    public static final DebugConfig DEFAULT = new DebugConfig(Collections.emptySet());

    /**
     * Creates config from YAML
     * @param section yaml
     * @return config
     */
    public static @NotNull DebugConfig from(@NotNull ConfigurationSection section) {
        return new DebugConfig(Arrays.stream(DebugOption.values())
                .filter(o -> section.getBoolean(o.config))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DebugOption.class))));
    }
}
