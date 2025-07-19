package kr.toxicity.model.api.util.lock;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

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
