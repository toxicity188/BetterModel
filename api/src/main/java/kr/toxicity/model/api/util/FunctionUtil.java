/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.util;

import kr.toxicity.model.api.util.function.BooleanConstantSupplier;
import kr.toxicity.model.api.util.function.FloatConstantSupplier;
import kr.toxicity.model.api.util.function.FloatSupplier;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
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
     * Takes value if predicate is matched
     * @param t t
     * @param predicate predicate
     * @return t or null
     * @param <T> type
     */
    public static <T> @Nullable T takeIf(@NotNull T t, @NotNull Predicate<T> predicate) {
        return predicate.test(t) ? t : null;
    }

    /**
     * Throttles this function by tick
     * @param <T> type
     * @param supplier target
     * @return throttled function
     */
    public static <T> @NotNull Supplier<T> throttleTick(@NotNull Supplier<T> supplier) {
        return throttleTick(MathUtil.MINECRAFT_TICK_MILLS, supplier);
    }

    /**
     * Throttles this function by tick
     * @param <T> type
     * @param tick tick
     * @param supplier target
     * @return throttled function
     */
    public static <T> @NotNull Supplier<T> throttleTick(long tick, @NotNull Supplier<T> supplier) {
        return supplier instanceof TickThrottledSupplier<T> throttledSupplier ? new TickThrottledSupplier<>(tick, throttledSupplier.delegate) : new TickThrottledSupplier<>(tick, supplier);
    }

    /**
     * Throttles this function by tick
     * @param supplier target
     * @return throttled function
     */
    public static @NotNull FloatSupplier throttleTickFloat(@NotNull FloatSupplier supplier) {
        return throttleTickFloat(MathUtil.MINECRAFT_TICK_MILLS, supplier);
    }

    /**
     * Throttles this function by tick
     * @param tick tick
     * @param supplier target
     * @return throttled function
     */
    public static @NotNull FloatSupplier throttleTickFloat(long tick, @NotNull FloatSupplier supplier) {
        return switch (supplier) {
            case TickThrottledFloatSupplier throttledSupplier -> new TickThrottledFloatSupplier(tick, throttledSupplier.delegate);
            case FloatConstantSupplier constantSupplier -> constantSupplier;
            default -> new TickThrottledFloatSupplier(tick, supplier);
        };
    }

    /**
     * Throttles this function by tick
     * @param supplier target
     * @return throttled function
     */
    public static @NotNull BooleanSupplier throttleTickBoolean(@NotNull BooleanSupplier supplier) {
        return switch (supplier) {
            case TickThrottledBooleanSupplier throttledSupplier -> throttledSupplier;
            case BooleanConstantSupplier booleanConstantSupplier -> booleanConstantSupplier;
            default -> new TickThrottledBooleanSupplier(supplier);
        };
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
        return throttleTick(MathUtil.MINECRAFT_TICK_MILLS, function);
    }

    /**
     * Throttles this function by tick
     * @param <T> from
     * @param <R> return
     * @param tick tick
     * @param function target
     * @return throttled function
     */
    public static <T, R> @NotNull Function<T, R> throttleTick(long tick, @NotNull Function<T, R> function) {
        return function instanceof TickThrottledFunction<T, R> throttledFunction ? new TickThrottledFunction<>(tick, throttledFunction.delegate) : new TickThrottledFunction<>(tick, function);
    }

    private static class TickThrottledSupplier<T> implements Supplier<T> {
        private final long tick;
        private final Supplier<T> delegate;
        private final AtomicLong time;
        private volatile T cache;

        public TickThrottledSupplier(long tick, @NotNull Supplier<T> delegate) {
            this.tick = tick;
            this.delegate = delegate;
            time = new AtomicLong(-tick - 1);
        }

        @Override
        public T get() {
            var old = time.get();
            var current = System.currentTimeMillis();
            if (current - old >= tick && time.compareAndSet(old, current)) cache = delegate.get();
            return cache;
        }
    }

    @RequiredArgsConstructor
    private static class TickThrottledFloatSupplier implements FloatSupplier {
        private final long tick;
        private final FloatSupplier delegate;
        private final AtomicLong time;
        private volatile float cache;

        public TickThrottledFloatSupplier(long tick, @NotNull FloatSupplier delegate) {
            this.tick = tick;
            this.delegate = delegate;
            time = new AtomicLong(-tick - 1);
        }

        @Override
        public float getAsFloat() {
            var old = time.get();
            var current = System.currentTimeMillis();
            if (current - old >= tick && time.compareAndSet(old, current)) cache = delegate.getAsFloat();
            return cache;
        }
    }

    @RequiredArgsConstructor
    private static class TickThrottledBooleanSupplier implements BooleanSupplier {
        private final @NotNull BooleanSupplier delegate;
        private final AtomicLong time = new AtomicLong(-51);
        private volatile boolean cache;

        @Override
        public boolean getAsBoolean() {
            var old = time.get();
            var current = System.currentTimeMillis();
            if (current - old >= 50 && time.compareAndSet(old, current)) cache = delegate.getAsBoolean();
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
        private final long tick;
        private final @NotNull Function<T, R> delegate;
        private final AtomicLong time;
        private volatile R cache;

        public TickThrottledFunction(long tick, @NotNull Function<T, R> delegate) {
            this.tick = tick;
            this.delegate = delegate;
            time = new AtomicLong(-tick - 1);
        }

        @Override
        public R apply(T t) {
            var old = time.get();
            var current = System.currentTimeMillis();
            if (current - old >= tick && time.compareAndSet(old, current)) cache = delegate.apply(t);
            return cache;
        }
    }
}
