/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.util.lock;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

@ApiStatus.Internal
public final class SingleLock {

    private final Object lock;

    public SingleLock() {
        this(null);
    }

    public SingleLock(@Nullable Object lock) {
        this.lock = lock != null ? lock : this;
    }

    public <T> T accessToLock(@NotNull Supplier<T> supplier) {
        synchronized (lock) {
            return supplier.get();
        }
    }
}
