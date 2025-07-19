package kr.toxicity.model.api.util.lock;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

public final class DuplexLock {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    public <T> T accessToReadLock(@NotNull Supplier<T> supplier) {
        var readLock = lock.readLock();
        readLock.lock();
        try {
            return supplier.get();
        } finally {
            readLock.unlock();
        }
    }
    
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
