package kr.toxicity.model.api.bukkit.scheduler;

import kr.toxicity.model.api.scheduler.ModelScheduler;
import kr.toxicity.model.api.scheduler.ModelTask;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface BukkitModelScheduler extends ModelScheduler {

    /**
     * Runs entity sync task
     * @param location location
     * @param runnable task
     * @return scheduled task
     */
    @Nullable ModelTask task(@NotNull Location location, @NotNull Runnable runnable);

    /**
     * Runs entity sync task
     * @param location location
     * @param delay delay
     * @param runnable task
     * @return scheduled task
     */
    @Nullable ModelTask taskLater(@NotNull Location location, long delay, @NotNull Runnable runnable);
}
