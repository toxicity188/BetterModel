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
 * Indicator config
 * @param options options
 */
public record IndicatorConfig(@NotNull Set<IndicatorOption> options) {
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
     * @param section yaml
     * @return config
     */
    public static @NotNull IndicatorConfig from(@NotNull ConfigurationSection section) {
        return new IndicatorConfig(Arrays.stream(IndicatorOption.values())
                .filter(o -> section.getBoolean(o.config))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(IndicatorOption.class))));
    }
}
