/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api;

import com.vdurmont.semver4j.Semver;
import kr.toxicity.model.api.manager.*;
import kr.toxicity.model.api.nms.NMS;
import kr.toxicity.model.api.pack.PackResult;
import kr.toxicity.model.api.pack.PackZipper;
import kr.toxicity.model.api.scheduler.ModelScheduler;
import kr.toxicity.model.api.version.MinecraftVersion;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.function.Consumer;

/**
 * A plugin instance of BetterModel.
 * It can be cast as JavaPlugin.
 * @see org.bukkit.plugin.java.JavaPlugin
 * @see BetterModel
 */
public interface BetterModelPlugin extends Plugin {

    /**
     * Reloads this plugin.
     * @return reload result
     */
    default @NotNull ReloadResult reload() {
        return reload(ReloadInfo.DEFAULT);
    }

    /**
     * Reloads this plugin.
     * @param sender sender
     * @return reload result
     */
    default @NotNull ReloadResult reload(@NotNull CommandSender sender) {
        return reload(ReloadInfo.builder().sender(sender).build());
    }

    /**
     * Reloads this plugin.
     * @param info info
     * @return reload result
     */
    @NotNull ReloadResult reload(@NotNull ReloadInfo info);


    /**
     * Check running BetterModel is snapshot.
     * @return is snapshot
     */
    boolean isSnapshot();

    /**
     * Gets BetterModel's config
     * @return config
     */
    @NotNull BetterModelConfig config();

    /**
     * Gets running server's minecraft version.
     * @return minecraft version
     */
    @NotNull MinecraftVersion version();

    /**
     * Gets plugin semver.
     * @return semver
     */
    @NotNull Semver semver();

    /**
     * Gets minecraft version volatile code.
     * @return net.minecraft.server
     */
    @NotNull NMS nms();

    /**
     * Gets model manager.
     * @return model manager
     */
    @NotNull ModelManager modelManager();
    /**
     * Gets player manager.
     * @return player manager
     */
    @NotNull PlayerManager playerManager();
    /**
     * Gets script manager.
     * @return script manager
     */
    @NotNull ScriptManager scriptManager();
    /**
     * Gets skin manager.
     * @return skin manager
     */
    @NotNull SkinManager skinManager();
    /**
     * Gets plugin scheduler.
     * @return scheduler
     */
    @NotNull ModelScheduler scheduler();

    /**
     * Adds event handler on reload start.
     * @param consumer task
     */
    void addReloadStartHandler(@NotNull Consumer<PackZipper> consumer);
    /**
     * Adds event handler on the reload end.
     * @param consumer result consumer
     */
    void addReloadEndHandler(@NotNull Consumer<ReloadResult> consumer);

    /**
     * Gets logger
     * @return logger
     */
    @NotNull BetterModelLogger logger();

    /**
     * Gets evaluator
     * @return evaluator
     */
    @NotNull BetterModelEvaluator evaluator();

    /**
     * Gets plugin resource from a path
     * @param path path
     */
    @Nullable InputStream getResource(@NotNull String path);

    /**
     * A result of reload.
     */
    sealed interface ReloadResult {

        /**
         * Reload success.
         * @param firstLoad first load
         * @param assetsTime assets reloading time
         * @param packResult pack result
         */
        record Success(boolean firstLoad, long assetsTime, @NotNull PackResult packResult) implements ReloadResult {

            /**
             * Gets packing time
             * @return packing time
             */
            public long packingTime() {
                return packResult().time();
            }

            /**
             * Gets total reload time
             * @return total reload time
             */
            public long totalTime() {
                return assetsTime + packingTime();
            }

            /**
             * Gets pack length
             * @return length
             */
            public long length() {
                var dir = packResult.directory();
                return dir != null && dir.isFile() ? dir.length() : packResult.stream().mapToLong(b -> b.bytes().length).sum();
            }
        }

        /**
         * Still on reload.
         */
        enum OnReload implements ReloadResult {
            /**
             * Singleton instance
             */
            INSTANCE
        }

        /**
         * Reload failure.
         * @param throwable reason
         */
        record Failure(@NotNull Throwable throwable) implements ReloadResult {
        }
    }
}
