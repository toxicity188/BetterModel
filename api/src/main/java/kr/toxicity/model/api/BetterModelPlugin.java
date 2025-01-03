package kr.toxicity.model.api;

import kr.toxicity.model.api.manager.*;
import kr.toxicity.model.api.nms.NMS;
import kr.toxicity.model.api.scheduler.ModelScheduler;
import kr.toxicity.model.api.version.MinecraftVersion;
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
    @NotNull ReloadResult reload();

    /**
     * Gets running server's minecraft version.
     * @return minecraft version
     */
    @NotNull MinecraftVersion version();

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
     */
    void addReloadStartHandler(@NotNull Runnable runnable);
    /**
     * Adds event handler on reload end.
     */
    void addReloadEndHandler(@NotNull Consumer<ReloadResult> consumer);

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
