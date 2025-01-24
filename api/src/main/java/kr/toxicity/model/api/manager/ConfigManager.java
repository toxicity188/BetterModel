package kr.toxicity.model.api.manager;

import kr.toxicity.model.api.config.DebugConfig;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

public interface ConfigManager {
    @NotNull DebugConfig debug();
    boolean metrics();
    boolean sightTrace();
    @NotNull Material item();
    double maxSight();
    double minSight();
    boolean lockOnPlayAnimation();
    long keyframeThreshold();
    boolean enablePlayerLimb();
    @NotNull String namespace();
    @NotNull PackType packType();
    @NotNull String buildFolderLocation();
    boolean disableGeneratingLegacyModels();

    enum PackType {
        FOLDER,
        ZIP
    }
}
