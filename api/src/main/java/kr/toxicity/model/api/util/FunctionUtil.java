package kr.toxicity.model.api.util;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class FunctionUtil {
    private FunctionUtil() {
        throw new RuntimeException();
    }

    public static <T> @NotNull Supplier<T> memoizeTick(@NotNull Supplier<T> supplier) {
        return supplier instanceof TickMemoizedSupplier<T> memoizedSupplier ? memoizedSupplier : new TickMemoizedSupplier<>(supplier);
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
}
