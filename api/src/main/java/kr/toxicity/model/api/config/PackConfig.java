package kr.toxicity.model.api.config;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public record PackConfig(
        boolean generateModernModel,
        boolean generateLegacyModel
) {

    public static final PackConfig DEFAULT = new PackConfig(true, true);

    public static @NotNull PackConfig from(@NotNull ConfigurationSection section) {
        return new PackConfig(
                section.getBoolean("generate-modern-model", true),
                section.getBoolean("generate-legacy-model", true)
        );
    }
}
