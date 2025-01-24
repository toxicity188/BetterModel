package kr.toxicity.model.api.config;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public record DebugConfig(
        boolean hitBox
) {
    public static final DebugConfig DEFAULT = new DebugConfig(
            false
    );
    public static @NotNull DebugConfig from(@NotNull ConfigurationSection section) {
        return new DebugConfig(
                section.getBoolean("hitbox")
        );
    }
}
