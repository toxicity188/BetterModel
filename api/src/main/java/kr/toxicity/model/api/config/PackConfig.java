package kr.toxicity.model.api.config;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

/**
 * Pack config
 * @param generateModernModel generate modern model
 * @param generateLegacyModel generate legacy model
 */
public record PackConfig(
        boolean generateModernModel,
        boolean generateLegacyModel
) {
    /**
     * Default config
     */
    public static final PackConfig DEFAULT = new PackConfig(true, true);

    /**
     * Creates config from YAML
     * @param section yaml
     * @return config
     */
    public static @NotNull PackConfig from(@NotNull ConfigurationSection section) {
        return new PackConfig(
                section.getBoolean("generate-modern-model", true),
                section.getBoolean("generate-legacy-model", true)
        );
    }
}
