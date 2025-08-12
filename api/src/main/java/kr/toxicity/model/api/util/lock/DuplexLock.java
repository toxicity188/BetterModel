package kr.toxicity.model.api.util.lock;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * Duplex lock
 */
@ApiStatus.Internal
public final class DuplexLock {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Access to read lock
     * @param supplier supplier
     * @return value
     * @param <T> type
     */
    public <T> T accessToReadLock(@NotNull Supplier<T> supplier) {
        var readLock = lock.readLock();
        readLock.lock();
        try {
            return supplier.get();
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Access to write lock
     * @param supplier supplier
     * @return value
     * @param <T> type
     */
    public <T> T accessToWriteLock(@NotNull Supplier<T> supplier) {
        var writeLock = lock.writeLock();
        writeLock.lock();
        try {
            return supplier.get();
        } finally {
            writeLock.unlock();
        }
    }
}
