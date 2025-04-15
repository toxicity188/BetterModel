package kr.toxicity.model.api.scheduler;

import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A scheduler of BetterModel
 */
public interface ModelScheduler {

    /**
     * Runs entity sync task
     * @param entity entity
     * @param runnable task
     * @return scheduled task
     */
    @Nullable ModelTask task(@NotNull Entity entity, @NotNull Runnable runnable);

    /**
     * Runs entity sync task
     * @param delay delay
     * @param entity entity
     * @param runnable task
     * @return scheduled task
     */
    @Nullable ModelTask taskLater(long delay, @NotNull Entity entity, @NotNull Runnable runnable);

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
