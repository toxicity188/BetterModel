package kr.toxicity.model.api.config;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

/**
 * Debug config
 * @param exception debug stack trace of exception
 * @param hitBox debug hit-box entity
 */
public record DebugConfig(
        boolean exception,
        boolean hitBox,
        boolean pack
) {
    /**
     * Default config
     */
    public static final DebugConfig DEFAULT = new DebugConfig(
            false,
            false,
            false
    );

    /**
     * Creates config from YAML
     * @param section yaml
     * @return config
     */
    public static @NotNull DebugConfig from(@NotNull ConfigurationSection section) {
        return new DebugConfig(
                section.getBoolean("exception"),
                section.getBoolean("hitbox"),
                section.getBoolean("pack")
        );
    }
}
