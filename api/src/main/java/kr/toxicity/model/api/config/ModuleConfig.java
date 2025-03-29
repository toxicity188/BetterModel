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
     * Creates config from yaml
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
