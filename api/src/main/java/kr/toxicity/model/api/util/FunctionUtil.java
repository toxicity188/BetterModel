package kr.toxicity.model.api.util;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class FunctionUtil {
    private FunctionUtil() {
        throw new RuntimeException();
    }

    public static @NotNull Runnable throttleTick(@NotNull Runnable runnable) {
        return runnable instanceof TickThrottledRunnable ThrottledRunnable ? ThrottledRunnable : new TickThrottledRunnable(runnable);
    }
    public static <T> @NotNull Supplier<T> throttleTick(@NotNull Supplier<T> supplier) {
        return supplier instanceof TickThrottledSupplier<T> ThrottledSupplier ? ThrottledSupplier : new TickThrottledSupplier<>(supplier);
    }
    public static <T> @NotNull Predicate<T> throttleTick(@NotNull Predicate<T> predicate) {
        return predicate instanceof TickThrottledPredicate<T> ThrottledSupplier ? ThrottledSupplier : new TickThrottledPredicate<>(predicate);
    }
    public static <T, R> @NotNull Function<T, R> throttleTick(@NotNull Function<T, R> function) {
        return function instanceof TickThrottledFunction<T, R> ThrottledFunction ? ThrottledFunction : new TickThrottledFunction<>(function);
    }

    @RequiredArgsConstructor
    private static class TickThrottledRunnable implements Runnable {
        private final @NotNull Runnable delegate;
        private final AtomicLong time = new AtomicLong();

        @Override
        public void run() {
            var old = time.get();
            var current = System.currentTimeMillis();
            if (current - old >= 50 && time.compareAndSet(old, current)) delegate.run();
        }
    }

    @RequiredArgsConstructor
    private static class TickThrottledSupplier<T> implements Supplier<T> {
        private final @NotNull Supplier<T> delegate;
        private final AtomicLong time = new AtomicLong();
        private volatile T cache;

        @Override
        public T get() {
            var old = time.get();
            var current = System.currentTimeMillis();
            if (current - old >= 50 && time.compareAndSet(old, current)) cache = delegate.get();
            return cache;
        }
    }

    @RequiredArgsConstructor
    private static class TickThrottledPredicate<T> implements Predicate<T> {
        private final @NotNull Predicate<T> delegate;
        private final AtomicLong time = new AtomicLong();
        private volatile boolean cache;

        @Override
        public boolean test(T t) {
            var old = time.get();
            var current = System.currentTimeMillis();
            if (current - old >= 50 && time.compareAndSet(old, current)) cache = delegate.test(t);
            return cache;
        }
    }

    @RequiredArgsConstructor
    private static class TickThrottledFunction<T, R> implements Function<T, R> {
        private final @NotNull Function<T, R> delegate;
        private final AtomicLong time = new AtomicLong();
        private volatile R cache;

        @Override
        public R apply(T t) {
            var old = time.get();
            var current = System.currentTimeMillis();
            if (current - old >= 50 && time.compareAndSet(old, current)) cache = delegate.apply(t);
            return cache;
        }
    }
}
