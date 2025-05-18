package kr.toxicity.model.api.util;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Function util
 */
@ApiStatus.Internal
public final class FunctionUtil {
    /**
     * No initializer
     */
    private FunctionUtil() {
        throw new RuntimeException();
    }

    /**
     * Makes constant value as supplier.
     * @param t value
     * @return supplier that returns always the same object.
     * @param <T> supplier
     */
    public static <T> @NotNull Supplier<T> asSupplier(@NotNull T t) {
        return () -> t;
    }

    /**
     * Throttles this function by tick
     * @param runnable target
     * @return throttled function
     */
    public static @NotNull Runnable throttleTick(@NotNull Runnable runnable) {
        return runnable instanceof TickThrottledRunnable throttledRunnable ? throttledRunnable : new TickThrottledRunnable(runnable);
    }

    /**
     * Throttles this function by tick
     * @param <T> type
     * @param supplier target
     * @return throttled function
     */
    public static <T> @NotNull Supplier<T> throttleTick(@NotNull Supplier<T> supplier) {
        return supplier instanceof TickThrottledSupplier<T> throttledSupplier ? throttledSupplier : new TickThrottledSupplier<>(supplier);
    }

    /**
     * Throttles this function by tick
     * @param <T> type
     * @param predicate target
     * @return throttled function
     */
    public static <T> @NotNull Predicate<T> throttleTick(@NotNull Predicate<T> predicate) {
        return predicate instanceof TickThrottledPredicate<T> throttledSupplier ? throttledSupplier : new TickThrottledPredicate<>(predicate);
    }

    /**
     * Throttles this function by tick
     * @param <T> from
     * @param <R> return
     * @param function target
     * @return throttled function
     */
    public static <T, R> @NotNull Function<T, R> throttleTick(@NotNull Function<T, R> function) {
        return function instanceof TickThrottledFunction<T, R> throttledFunction ? throttledFunction : new TickThrottledFunction<>(function);
    }

    @RequiredArgsConstructor
    private static class TickThrottledRunnable implements Runnable {
        private final @NotNull Runnable delegate;
        private final AtomicLong time = new AtomicLong(-51);

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
        private final AtomicLong time = new AtomicLong(-51);
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
        private final AtomicLong time = new AtomicLong(-51);
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
        private final AtomicLong time = new AtomicLong(-51);
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
