package kr.toxicity.model.api.util;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Supplier;

public class FunctionUtil {
    private FunctionUtil() {
        throw new RuntimeException();
    }

    public static <T> @NotNull Supplier<T> memoizeTick(@NotNull Supplier<T> supplier) {
        return supplier instanceof TickMemoizedSupplier<T> memoizedSupplier ? memoizedSupplier : new TickMemoizedSupplier<>(supplier);
    }
    public static <T, R> @NotNull Function<T, R> memoizeTick(@NotNull Function<T, R> function) {
        return function instanceof TickMemoizedFunction<T, R> memoizedFunction ? memoizedFunction : new TickMemoizedFunction<>(function);
    }

    @RequiredArgsConstructor
    private static class TickMemoizedSupplier<T> implements Supplier<T> {
        private final @NotNull Supplier<T> delegate;
        private volatile long time;
        private volatile T cache;

        @Override
        public synchronized T get() {
            var current = System.currentTimeMillis();
            if (current - time >= 50) {
                time = current;
                cache = delegate.get();
            }
            return cache;
        }
    }

    @RequiredArgsConstructor
    private static class TickMemoizedFunction<T, R> implements Function<T, R> {
        private final @NotNull Function<T, R> delegate;
        private volatile long time;
        private volatile R cache;

        @Override
        public synchronized R apply(T t) {
            var current = System.currentTimeMillis();
            if (current - time >= 50) {
                time = current;
                cache = delegate.apply(t);
            }
            return cache;
        }
    }
}
