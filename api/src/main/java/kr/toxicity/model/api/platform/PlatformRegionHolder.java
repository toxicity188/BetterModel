/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.platform;

import kr.toxicity.model.api.scheduler.ModelTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an object that holds a region context (e.g., an entity or location) for task scheduling.
 * <p>
 * This interface is crucial for platforms like Folia where tasks must be scheduled relative to a specific region.
 * </p>
 *
 * @since 2.0.0
 */
public interface PlatformRegionHolder {
    /**
     * Schedules a task to run on the next tick, synchronized with this region holder.
     *
     * @param runnable the task to run
     * @return the scheduled task, or null if scheduling failed
     * @since 2.0.0
     */
    @Nullable ModelTask task(@NotNull Runnable runnable);

    /**
     * Schedules a task to run after a delay, synchronized with this region holder.
     *
     * @param delay the delay in ticks
     * @param runnable the task to run
     * @return the scheduled task, or null if scheduling failed
     * @since 2.0.0
     */
    @Nullable ModelTask taskLater(long delay, @NotNull Runnable runnable);
}
