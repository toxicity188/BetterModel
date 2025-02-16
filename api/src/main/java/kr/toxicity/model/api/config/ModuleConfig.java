package kr.toxicity.model.api.config;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public record ModuleConfig(
        boolean model,
        boolean playerAnimation
) {
    public static final ModuleConfig DEFAULT = new ModuleConfig(
            true,
            true
    );
    public static @NotNull ModuleConfig from(@NotNull ConfigurationSection section) {
        return new ModuleConfig(
                section.getBoolean("model"),
                section.getBoolean("player-animation")
        );
    }
}
