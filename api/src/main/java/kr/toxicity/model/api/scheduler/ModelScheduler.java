/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.scheduler;

import kr.toxicity.model.api.entity.BaseEntity;
import org.bukkit.Location;
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
    default @Nullable ModelTask task(@NotNull Entity entity, @NotNull Runnable runnable) {
        return task(entity.getLocation(), runnable);
    }

    /**
     * Runs entity sync task
     * @param entity entity
     * @param delay delay
     * @param runnable task
     * @return scheduled task
     */
    default @Nullable ModelTask taskLater(@NotNull Entity entity, long delay, @NotNull Runnable runnable) {
        return taskLater(entity.getLocation(), delay, runnable);
    }

    /**
     * Runs entity sync task
     * @param entity entity
     * @param runnable task
     * @return scheduled task
     */
    default @Nullable ModelTask task(@NotNull BaseEntity entity, @NotNull Runnable runnable) {
        return task(entity.location(), runnable);
    }

    /**
     * Runs entity sync task
     * @param entity entity
     * @param delay delay
     * @param runnable task
     * @return scheduled task
     */
    default @Nullable ModelTask taskLater(@NotNull BaseEntity entity, long delay, @NotNull Runnable runnable) {
        return taskLater(entity.location(), delay, runnable);
    }

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
