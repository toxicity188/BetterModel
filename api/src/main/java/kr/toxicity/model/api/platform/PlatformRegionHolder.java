package kr.toxicity.model.api.platform;

import kr.toxicity.model.api.scheduler.ModelTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PlatformRegionHolder {
    /**
     * Runs entity sync task
     * @param runnable task
     * @return scheduled task
     */
    @Nullable ModelTask task(@NotNull Runnable runnable);

    /**
     * Runs entity sync task
     * @param delay delay
     * @param runnable task
     * @return scheduled task
     */
    @Nullable ModelTask taskLater(long delay, @NotNull Runnable runnable);
}
