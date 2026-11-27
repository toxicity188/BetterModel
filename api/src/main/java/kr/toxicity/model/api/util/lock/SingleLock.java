/*
 * This source file is part of BetterModel.
 * Copyright (c) 2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.util.lock;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Serializes operations using a Java monitor.
 */
@ApiStatus.Internal
public final class SingleLock {

    private final Object lock;

    /**
     * Creates a lock with an independent monitor.
     */
    public SingleLock() {
        this(null);
    }

    /**
     * Creates a lock using the supplied monitor.
     *
     * @param lock the monitor, or {@code null} to use an independent monitor
     */
    public SingleLock(@Nullable Object lock) {
        this.lock = lock != null ? lock : this;
    }

    /**
     * Executes an operation while holding the monitor.
     *
     * @param supplier the operation to run
     * @param <T> the result type
     * @return the result from the supplier
     */
    public <T> T accessToLock(@NotNull Supplier<T> supplier) {
        synchronized (lock) {
            return supplier.get();
        }
    }
}
