package kr.toxicity.model.api.manager;

import kr.toxicity.model.api.config.DebugConfig;
import kr.toxicity.model.api.config.ModuleConfig;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

/**
 * Config manager
 */
public interface ConfigManager {

    /**
     * Gets debug config
     * @return debug config
     */
    @NotNull DebugConfig debug();

    /**
     * Gets module config
     * @return module config
     */
    @NotNull ModuleConfig module();

    /**
     * Checks metrics is enabled
     * @return enabled
     */
    boolean metrics();

    /**
     * Checks sight trace is enabled
     * @return enabled
     */
    boolean sightTrace();

    /**
     * Checks resource pack target item type
     * @return material
     */
    @NotNull Material item();

    /**
     * Gets max range of sight trace
     * @return max
     */
    double maxSight();

    /**
     * Gets min range of sight trace
     * @return min
     */
    double minSight();

    /**
     * Locks rotating model when plays animation by play once
     * @return lock
     */
    boolean lockOnPlayAnimation();

    /**
     * Gets namespace of resource pack
     * @return namespace
     */
    @NotNull String namespace();

    /**
     * Gets pack type
     * @return type
     */
    @NotNull PackType packType();

    /**
     * Gets build folder location
     * @return build folder
     */
    @NotNull String buildFolderLocation();

    /**
     * Checks generating legacy model data is disabled
     * @return generating legacy model
     */
    boolean disableGeneratingLegacyModels();

    /**
     * Checks model tracker will follow source entity's invisibility
     * @return follow invisibility
     */
    boolean followMobInvisibility();

    /**
     * Gets animation time in texture mcmeta
     * @return animation time
     */
    int animatedTextureFrameTime();

    /**
     * Checks whether BetterModel should create pack.mcmeta or not
     * @return create pack mcmeta
     */
    boolean createPackMcmeta();

    /**
     * Pack type
     */
    enum PackType {
        /**
         * Build to folder
         */
        FOLDER,
        /**
         * Build to zip
         */
        ZIP
    }
}
