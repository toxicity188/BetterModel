/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.config;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Indicator config
 * @param options options
 */
public record IndicatorConfig(@NotNull @Unmodifiable Set<IndicatorOption> options) {
    /**
     * Indicator option
     */
    @RequiredArgsConstructor
    public enum IndicatorOption {
        /**
         * Progress bar
         */
        PROGRESS_BAR("progress_bar"),
        ;
        private final String config;
    }

    /**
     * Default config
     */
    public static final IndicatorConfig DEFAULT = new IndicatorConfig(Collections.emptySet());

    /**
     * Creates config from YAML
     * @param predicate predicate
     * @return config
     */
    public static @NotNull IndicatorConfig from(@NotNull Predicate<String> predicate) {
        return new IndicatorConfig(Collections.unmodifiableSet(Arrays.stream(IndicatorOption.values())
            .filter(o -> predicate.test(o.config))
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(IndicatorOption.class)))));
    }
}
