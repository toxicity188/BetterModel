package kr.toxicity.model.api;

import kr.toxicity.model.api.config.DebugConfig;
import kr.toxicity.model.api.config.IndicatorConfig;
import kr.toxicity.model.api.config.ModuleConfig;
import kr.toxicity.model.api.mount.MountController;
import kr.toxicity.model.api.config.PackConfig;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

/**
 * BetterModel's config
 */
public interface BetterModelConfig {

    /**
     * Gets debug config
     * @return debug config
     */
    @NotNull DebugConfig debug();

    /**
     * Gets indicator config
     * @return indicator config
     */
    @NotNull IndicatorConfig indicator();

    /**
     * Gets module config
     * @return module config
     */
    @NotNull ModuleConfig module();

    /**
     * Gets pack config
     * @return pack config
     */
    @NotNull PackConfig pack();

    /**
     * Checks metrics is enabled
     * @return enabled
     */
    boolean metrics();

    /**
     * Check sight trace is enabled
     * @return enabled
     */
    boolean sightTrace();

    /**
     * Checks resource pack target item type
     * @return material
     */
    @NotNull Material item();

    /**
     * Gets item namespace
     * @return item namespace
     */
    @NotNull String itemNamespace();

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
     * Gets namespace of resource pack
     * @return namespace
     */
    @NotNull String namespace();

    /**
     * Gets a pack type
     * @return type
     */
    @NotNull PackType packType();

    /**
     * Gets build folder location
     * @return build folder
     */
    @NotNull String buildFolderLocation();

    /**
     * Check model tracker will follow source entity's invisibility
     * @return follow invisibility
     */
    boolean followMobInvisibility();

    /**
     * Gets animation time in texture mcmeta
     * @return animation time
     */
    int animatedTextureFrameTime();

    /**
     * Checks use Purpur afk.
     * @return use Purpur afk
     */
    boolean usePurpurAfk();

    /**
     * Should BetterModel sends version update message when op has joined.
     * @return send or not
     */
    boolean versionCheck();

    /**
     * Gets the default mount controller
     * @see kr.toxicity.model.api.mount.MountControllers
     * @return mount controller
     */
    @NotNull MountController defaultMountController();

    /**
     * Gets lerp frame time
     * @return lerp frame time
     */
    int lerpFrameTime();

    /**
     * Whether any swap inventory packet should be canceled if the player has some model
     * @return cancel
     */
    boolean cancelPlayerModelInventory();

    /**
     * Gets entity hide delay of player
     * @return player hide
     */
    long playerHideDelay();

    /**
     * Gets packet bundling size
     * @return packet bundling size
     */
    int packetBundlingSize();

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
        ZIP,
        /**
         * Nothing to build
         */
        NONE
    }
}
