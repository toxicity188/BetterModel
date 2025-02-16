package kr.toxicity.model.api.manager;

import kr.toxicity.model.api.config.DebugConfig;
import kr.toxicity.model.api.config.ModuleConfig;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

public interface ConfigManager {
    @NotNull DebugConfig debug();
    @NotNull ModuleConfig module();
    boolean metrics();
    boolean sightTrace();
    @NotNull Material item();
    double maxSight();
    double minSight();
    boolean lockOnPlayAnimation();
    float keyframeThreshold();
    @NotNull String namespace();
    @NotNull PackType packType();
    @NotNull String buildFolderLocation();
    boolean disableGeneratingLegacyModels();
    boolean followMobInvisibility();

    enum PackType {
        FOLDER,
        ZIP
    }
}
