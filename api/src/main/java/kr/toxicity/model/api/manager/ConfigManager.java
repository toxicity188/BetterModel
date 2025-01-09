package kr.toxicity.model.api.manager;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

public interface ConfigManager {
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
