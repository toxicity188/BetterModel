/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api;

import kr.toxicity.model.api.config.DebugConfig;
import kr.toxicity.model.api.config.IndicatorConfig;
import kr.toxicity.model.api.config.ModuleConfig;
import kr.toxicity.model.api.config.PackConfig;
import kr.toxicity.model.api.mount.MountController;
import kr.toxicity.model.api.platform.PlatformItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Represents the main configuration interface for BetterModel.
 * <p>
 * This interface provides access to various configuration settings, including debug options,
 * pack generation settings, module toggles, and runtime behaviors.
 * </p>
 *
 * @since 1.15.2
 */
public interface BetterModelConfig {

    /**
     * Returns the debug configuration.
     *
     * @return the debug config
     * @since 1.15.2
     */
    @NotNull DebugConfig debug();

    /**
     * Returns the indicator configuration.
     *
     * @return the indicator config
     * @since 1.15.2
     */
    @NotNull IndicatorConfig indicator();

    /**
     * Returns the module configuration.
     *
     * @return the module config
     * @since 1.15.2
     */
    @NotNull ModuleConfig module();

    /**
     * Returns the resource pack configuration.
     *
     * @return the pack config
     * @since 1.15.2
     */
    @NotNull PackConfig pack();

    /**
     * Checks if metrics collection is enabled.
     *
     * @return true if enabled, false otherwise
     * @since 1.15.2
     */
    boolean metrics();

    /**
     * Checks if sight tracing (visibility checking) is enabled.
     *
     * @return true if enabled, false otherwise
     * @since 1.15.2
     */
    boolean sightTrace();

    /**
     * Checks if BetterModel should attempt to merge its resource pack with external plugins/mods.
     *
     * @return true to merge, false otherwise
     * @since 1.15.2
     */
    boolean mergeWithExternalResources();

    /**
     * Returns a supplier for the platform item stack used as the base for model items.
     *
     * @return a supplier providing the target item stack
     * @since 2.0.0
     */
    @NotNull Supplier<PlatformItemStack> item();

    /**
     * Returns the item model string identifier used for the resource pack target item.
     *
     * @return the item model string
     * @since 2.0.0
     */
    @NotNull String itemModel();

    /**
     * Returns the namespace used for the target item.
     *
     * @return the item namespace
     * @since 1.15.2
     */
    @NotNull String itemNamespace();

    /**
     * Returns the maximum range for sight tracing.
     *
     * @return the max range
     * @since 1.15.2
     */
    double maxSight();

    /**
     * Returns the minimum range for sight tracing.
     *
     * @return the min range
     * @since 1.15.2
     */
    double minSight();

    /**
     * Returns the namespace used for the generated resource pack.
     *
     * @return the namespace
     * @since 1.15.2
     */
    @NotNull String namespace();

    /**
     * Returns the type of resource pack generation (Folder, Zip, or None).
     *
     * @return the pack type
     * @since 1.15.2
     */
    @NotNull PackType packType();

    /**
     * Returns the location of the build folder for resource packs.
     *
     * @return the build folder path
     * @since 1.15.2
     */
    @NotNull String buildFolderLocation();

    /**
     * Checks if model trackers should follow the source entity's invisibility status.
     *
     * @return true to follow invisibility, false otherwise
     * @since 1.15.2
     */
    boolean followMobInvisibility();

    /**
     * Checks if Purpur's AFK API should be used.
     *
     * @return true to use Purpur AFK, false otherwise
     * @since 1.15.2
     */
    boolean usePurpurAfk();

    /**
     * Checks if version update notifications should be sent to OPs on join.
     *
     * @return true to send notifications, false otherwise
     * @since 1.15.2
     */
    boolean versionCheck();

    /**
     * Returns the default mount controller used for entities.
     *
     * @return the default mount controller
     * @see kr.toxicity.model.api.mount.MountControllers
     * @since 1.15.2
     */
    @NotNull MountController defaultMountController();

    /**
     * Returns the interpolation frame time (lerp) in milliseconds.
     *
     * @return the lerp frame time
     * @since 1.15.2
     */
    int lerpFrameTime();

    /**
     * Checks if inventory swap packets should be cancelled for players with active models.
     *
     * @return true to cancel, false otherwise
     * @since 1.15.2
     */
    boolean cancelPlayerModelInventory();

    /**
     * Returns the delay in ticks before hiding a player's model after they become invisible.
     *
     * @return the hide delay
     * @since 1.15.2
     */
    long playerHideDelay();

    /**
     * Returns the threshold size for packet bundling.
     *
     * @return the packet bundling size
     * @since 1.15.2
     */
    int packetBundlingSize();

    /**
     * Checks if strict loading mode is enabled.
     * <p>
     * Strict loading causes the platform to fail fast on model loading errors.
     * </p>
     *
     * @return true if strict loading is enabled, false otherwise
     * @since 1.15.2
     */
    boolean enableStrictLoading();

    /**
     * Enumerates the types of resource pack generation.
     *
     * @since 1.15.2
     */
    enum PackType {
        /**
         * Generate the resource pack as a folder structure.
         * @since 1.15.2
         */
        FOLDER,
        /**
         * Generate the resource pack as a ZIP archive.
         * @since 1.15.2
         */
        ZIP,
        /**
         * Do not generate a resource pack.
         * @since 1.15.2
         */
        NONE
    }
}
