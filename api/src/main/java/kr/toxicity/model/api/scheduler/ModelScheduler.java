package kr.toxicity.model.api.scheduler;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * A scheduler of BetterModel
 */
public interface ModelScheduler {

    /**
     * Runs sync task
     * @param location location
     * @param runnable task
     * @return scheduled task
     */
    @NotNull ModelTask task(@NotNull Location location, @NotNull Runnable runnable);

    /**
     * Runs sync task
     * @param delay delay
     * @param location location
     * @param runnable task
     * @return scheduled task
     */
    @NotNull ModelTask taskLater(long delay, @NotNull Location location, @NotNull Runnable runnable);

    /**
     * Runs async task
     * @param runnable task
     * @return scheduled task
     */
    @NotNull ModelTask asyncTask(@NotNull Runnable runnable);

    /**
     * Runs async task
     * @param delay delay
     * @param runnable task
     * @return scheduled task
     */
    @NotNull ModelTask asyncTaskLater(long delay, @NotNull Runnable runnable);

    /**
     * Runs async task
     * @param delay delay
     * @param period period
     * @param runnable task
     * @return scheduled task
     */
    @NotNull ModelTask asyncTaskTimer(long delay, long period, @NotNull Runnable runnable);
}
