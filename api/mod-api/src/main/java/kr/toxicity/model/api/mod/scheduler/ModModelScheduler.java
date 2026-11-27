/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.mod.scheduler;

import kr.toxicity.model.api.scheduler.ModelScheduler;
import kr.toxicity.model.api.scheduler.ModelTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a Mod-specific scheduler for model tasks.
 * <p>
 * This interface extends {@link ModelScheduler} to provide methods for scheduling tasks
 * within the Mod environment.
 * </p>
 *
 * @since 2.0.0
 */
public interface ModModelScheduler extends ModelScheduler {

    /**
     * Queues a task on the Minecraft server thread for the next scheduler tick.
     *
     * @param runnable the task to run
     * @return the scheduled task, or {@code null} if the scheduler is disabled
     * @since 2.0.0
     */
    @Nullable ModelTask task(@NotNull Runnable runnable);

    /**
     * Queues a task on the Minecraft server thread after a delay.
     *
     * @param delay the delay in ticks
     * @param runnable the task to run
     * @return the scheduled task, or {@code null} if the scheduler is disabled
     * @since 2.0.0
     */
    @Nullable ModelTask taskLater(long delay, @NotNull Runnable runnable);
}
