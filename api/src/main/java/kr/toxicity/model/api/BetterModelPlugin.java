package kr.toxicity.model.api;

import com.vdurmont.semver4j.Semver;
import kr.toxicity.model.api.manager.*;
import kr.toxicity.model.api.nms.NMS;
import kr.toxicity.model.api.scheduler.ModelScheduler;
import kr.toxicity.model.api.version.MinecraftVersion;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * A plugin instance of BetterModel.
 * It can be cast as JavaPlugin.
 * @see org.bukkit.plugin.java.JavaPlugin
 * @see BetterModel
 */
public interface BetterModelPlugin {

    /**
     * Reloads this plugin.
     * @return reload result
     */
    default @NotNull ReloadResult reload() {
        return reload(ReloadInfo.DEFAULT);
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
     * Gets entity manager.
     * @return entity manager
     */
    @NotNull EntityManager entityManager();
    /**
     * Gets command manager.
     * @return command manager
     */
    @NotNull CommandManager commandManager();
    /**
     * Gets script manager.
     * @return script manager
     */
    @NotNull ScriptManager scriptManager();
    /**
     * Gets compatibility manager.
     * @return compatibility manager
     */
    @NotNull CompatibilityManager compatibilityManager();
    /**
     * Gets config manager.
     * @return config manager
     */
    @NotNull ConfigManager configManager();
    /**
     * Gets plugin scheduler.
     * @return scheduler
     */
    @NotNull ModelScheduler scheduler();

    /**
     * Adds event handler on reload start.
     * @param runnable task
     */
    void addReloadStartHandler(@NotNull Runnable runnable);
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
     * Gets bukkit audiences
     * @return bukkit audiences
     */
    @NotNull BukkitAudiences audiences();

    /**
     * A result of reload.
     */
    sealed interface ReloadResult {

        /**
         * Reload success.
         * @param time total time
         */
        record Success(long time) implements ReloadResult {
        }

        /**
         * Still on reload.
         */
        record OnReload() implements ReloadResult {
        }

        /**
         * Reload failure.
         * @param throwable reason
         */
        record Failure(@NotNull Throwable throwable) implements ReloadResult {
        }
    }
}
