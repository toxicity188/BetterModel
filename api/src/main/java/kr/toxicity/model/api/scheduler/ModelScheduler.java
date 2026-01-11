/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
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
