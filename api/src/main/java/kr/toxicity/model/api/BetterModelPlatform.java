/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api;

import com.vdurmont.semver4j.Semver;
import kr.toxicity.model.api.event.ModelEventApplication;
import kr.toxicity.model.api.manager.*;
import kr.toxicity.model.api.nms.NMS;
import kr.toxicity.model.api.pack.PackResult;
import kr.toxicity.model.api.pack.PackZipper;
import kr.toxicity.model.api.platform.PlatformAdapter;
import kr.toxicity.model.api.scheduler.ModelScheduler;
import kr.toxicity.model.api.version.MinecraftVersion;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.util.function.Consumer;

/**
 * Represents the main platform interface for BetterModel.
 *
 * @see BetterModel
 * @since 1.15.2
 */
public interface BetterModelPlatform extends ModelEventApplication {

    /**
     * Returns the data folder for the BetterModel plugin.
     * This is where configuration files, data files, and other plugin-specific resources are stored.
     *
     * @return the data folder as a {@link File} object.
     * @since 2.0.0
     */
    @NotNull File dataFolder();

    /**
     * Returns the type of JAR file this platform is running on (e.g., SPIGOT, PAPER, FABRIC).
     *
     * @return the {@link JarType} enum representing the platform's JAR type.
     * @since 2.0.0
     */
    @NotNull JarType jarType();

    /**
     * Reloads the platform with default settings (console sender).
     *
     * @return the result of the reload operation
     * @since 2.0.0
     */
    default @NotNull ReloadResult reload() {
        return reload(ReloadInfo.DEFAULT);
    }

    /**
     * Reloads the platform, specifying the command sender who initiated it.
     *
     * @param sender the command sender
     * @return the result of the reload operation
     * @since 1.15.2
     */
    default @NotNull ReloadResult reload(@NotNull Audience sender) {
        return reload(ReloadInfo.builder().sender(sender).build());
    }

    /**
     * Reloads the platform with specific reload information.
     *
     * @param info the reload configuration
     * @return the result of the reload operation
     * @since 1.15.2
     */
    @NotNull ReloadResult reload(@NotNull ReloadInfo info);


    /**
     * Checks if the running version of BetterModel is a snapshot build.
     *
     * @return true if snapshot, false otherwise
     * @since 1.15.2
     */
    boolean isSnapshot();

    /**
     * Returns the platform's configuration manager.
     *
     * @return the configuration
     * @since 1.15.2
     */
    @NotNull BetterModelConfig config();

    /**
     * Returns the Minecraft version of the running server.
     *
     * @return the Minecraft version
     * @since 1.15.2
     */
    @NotNull MinecraftVersion version();

    /**
     * Returns the semantic version of the platform.
     *
     * @return the semantic version
     * @since 1.15.2
     */
    @NotNull Semver semver();

    /**
     * Returns the NMS (Net.Minecraft.Server) handler for version-specific operations.
     *
     * @return the NMS handler
     * @since 1.15.2
     */
    @NotNull NMS nms();

    /**
     * Returns the model manager.
     *
     * @return the model manager
     * @since 1.15.2
     */
    @NotNull ModelManager modelManager();

    /**
     * Returns the player manager.
     *
     * @return the player manager
     * @since 1.15.2
     */
    @NotNull PlayerManager playerManager();

    /**
     * Returns the script manager.
     *
     * @return the script manager
     * @since 1.15.2
     */
    @NotNull ScriptManager scriptManager();

    /**
     * Returns the skin manager.
     *
     * @return the skin manager
     * @since 1.15.2
     */
    @NotNull SkinManager skinManager();
    /**
     * Returns the profile manager.
     *
     * @return the profile manager
     * @since 1.15.2
     */
    @NotNull ProfileManager profileManager();

    /**
     * Returns the platform's scheduler.
     *
     * @return the scheduler
     * @since 1.15.2
     */
    @NotNull ModelScheduler scheduler();

    /**
     * Return the platform's adapter
     * @return the adapter
     */
    @NotNull PlatformAdapter adapter();

    /**
     * Registers a handler to be executed when a reload starts.
     *
     * @param consumer the handler, receiving the {@link PackZipper}
     * @since 1.15.2
     */
    void addReloadStartHandler(@NotNull Consumer<PackZipper> consumer);

    /**
     * Registers a handler to be executed when a reload ends.
     *
     * @param consumer the handler, receiving the {@link ReloadResult}
     * @since 1.15.2
     */
    void addReloadEndHandler(@NotNull Consumer<ReloadResult> consumer);

    /**
     * Returns the platform's logger.
     *
     * @return the logger
     * @since 1.15.2
     */
    @NotNull BetterModelLogger logger();

    /**
     * Returns the expression evaluator.
     *
     * @return the evaluator
     * @since 1.15.2
     */
    @NotNull BetterModelEvaluator evaluator();

    /**
     * Returns the event bus.
     *
     * @return the event bus
     * @since 2.0.0
     */
    @NotNull BetterModelEventBus eventBus();

    /**
     * Retrieves a resource from the platform's JAR file.
     *
     * @param path the path to the resource
     * @return an input stream for the resource, or null if not found
     * @since 1.15.2
     */
    @Nullable InputStream getResource(@NotNull String path);

    /**
     * Represents the outcome of a platform reload operation.
     *
     * @since 1.15.2
     */
    sealed interface ReloadResult {

        /**
         * Indicates a successful reload.
         *
         * @param firstLoad true if this is the first load (startup), false otherwise
         * @param assetsTime the time taken to reload assets in milliseconds
         * @param packResult the result of the resource pack generation
         * @since 1.15.2
         */
        record Success(boolean firstLoad, long assetsTime, @NotNull PackResult packResult) implements ReloadResult {

            /**
             * Returns the time taken to generate the resource pack.
             *
             * @return the packing time in milliseconds
             * @since 1.15.2
             */
            public long packingTime() {
                return packResult().time();
            }

            /**
             * Returns the total time taken for the reload operation.
             *
             * @return the total time in milliseconds
             * @since 1.15.2
             */
            public long totalTime() {
                return assetsTime + packingTime();
            }

            /**
             * Returns the size of the generated resource pack.
             *
             * @return the size in bytes
             * @since 1.15.2
             */
            public long length() {
                var dir = packResult.directory();
                return dir != null && dir.isFile() ? dir.length() : packResult.stream().mapToLong(b -> b.bytes().length).sum();
            }
        }

        /**
         * Indicates that a reload is currently in progress.
         * @since 1.15.2
         */
        enum OnReload implements ReloadResult {
            /**
             * Singleton instance.
             * @since 1.15.2
             */
            INSTANCE
        }

        /**
         * Indicates a failed reload.
         *
         * @param throwable the exception that caused the failure
         * @since 1.15.2
         */
        record Failure(@NotNull Throwable throwable) implements ReloadResult {
        }
    }

    /**
     * Represents the type of JAR file the platform is running on.
     * This enum helps identify the specific server implementation (e.g., Spigot, Paper, Fabric).
     *
     * @since 2.0.0
     */
    @RequiredArgsConstructor
    enum JarType {
        /**
         * Indicates a Spigot-based server.
         * @since 2.0.0
         */
        SPIGOT("spigot"),
        /**
         * Indicates a Paper-based server.
         * @since 2.0.0
         */
        PAPER("paper"),
        /**
         * Indicates a Fabric-based server.
         * @since 2.0.0
         */
        FABRIC("fabric");

        private final String raw;

        /**
         * Returns the raw string representation of the JAR type.
         *
         * @return the raw string (e.g., "spigot", "paper", "fabric")
         * @since 2.0.0
         */
        public String raw() {
            return raw;
        }
    }
}
