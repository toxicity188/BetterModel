package kr.toxicity.model.api.scheduler;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public interface ModelScheduler {
    @NotNull ModelTask task(@NotNull Location location, @NotNull Runnable runnable);
    @NotNull ModelTask taskLater(long delay, @NotNull Location location, @NotNull Runnable runnable);
    @NotNull ModelTask asyncTask(@NotNull Runnable runnable);
    @NotNull ModelTask asyncTaskLater(long delay, @NotNull Runnable runnable);
    @NotNull ModelTask asyncTaskTimer(long delay, long period, @NotNull Runnable runnable);
}
