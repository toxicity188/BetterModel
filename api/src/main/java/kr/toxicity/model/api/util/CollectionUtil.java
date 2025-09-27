/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import it.unimi.dsi.fastutil.floats.FloatCollection;
import kr.toxicity.model.api.BetterModel;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Collection util
 */
@ApiStatus.Internal
public final class CollectionUtil {
    /**
     * No initializer
     */
    private CollectionUtil() {
        throw new RuntimeException();
    }

    /**
     * Filters stream by some instance
     * @param collection collection
     * @param rClass class of instance
     * @return filtered stream
     * @param <E> element
     * @param <R> return value
     */
    @NotNull
    public static <E, R> Stream<R> filterIsInstance(@NotNull Collection<E> collection, @NotNull Class<R> rClass) {
        return filterIsInstance(collection.stream(), rClass);
    }

    /**
     * Maps stream to list
     * @param collection collection
     * @param mapper mapper
     * @return unmodifiable list
     * @param <E> element
     * @param <R> return value
     */
    @NotNull
    @Unmodifiable
    public static <E, R> List<R> mapToList(@NotNull Collection<E> collection, @NotNull Function<E, R> mapper) {
        return mapToList(collection.stream(), mapper);
    }

    /**
     * Maps stream to list
     * @param stream stream
     * @param mapper mapper
     * @return unmodifiable list
     * @param <E> element
     * @param <R> return value
     */
    @NotNull
    @Unmodifiable
    public static <E, R> List<R> mapToList(@NotNull Stream<E> stream, @NotNull Function<E, R> mapper) {
        return stream.map(mapper).toList();
    }

    /**
     * Maps stream to JSON
     * @param collection collection
     * @param mapper mapper
     * @return JSON array
     * @param <E> element
     * @param <R> return value
     */
    @NotNull
    public static <E, R extends JsonElement> JsonArray mapToJson(@NotNull Collection<E> collection, @NotNull Function<E, R> mapper) {
        return mapToJson(collection.size(), collection.stream(), mapper);
    }

    /**
     * Maps stream to JSON
     * @param stream stream
     * @param mapper mapper
     * @return JSON array
     * @param <E> element
     * @param <R> return value
     */
    @NotNull
    public static <E, R extends JsonElement> JsonArray mapToJson(@NotNull Stream<E> stream, @NotNull Function<E, R> mapper) {
        return mapToJson(10, stream, mapper);
    }

    /**
     * Maps stream to JSON
     * @param capacity initial capacity
     * @param stream stream
     * @param mapper mapper
     * @return JSON array
     * @param <E> element
     * @param <R> return value
     */
    @NotNull
    public static <E, R extends JsonElement> JsonArray mapToJson(int capacity, @NotNull Stream<E> stream, @NotNull Function<E, R> mapper) {
        var array = new JsonArray(capacity);
        stream.map(mapper).forEach(array::add);
        return array;
    }

    /**
     * Filters stream by some instance
     * @param stream stream
     * @param rClass class of instance
     * @return filtered stream
     * @param <E> element
     * @param <R> return value
     */
    @NotNull
    public static <E, R> Stream<R> filterIsInstance(@NotNull Stream<E> stream, @NotNull Class<R> rClass) {
        return stream.filter(rClass::isInstance)
                .map(rClass::cast);
    }

    /**
     * Groups stream by some key
     * @param stream stream
     * @param keyMapper key mapper
     * @return unmodifiable grouped map
     * @param <K> key
     * @param <E> element
     */
    @NotNull
    @Unmodifiable
    public static <K, E> Map<K, List<E>> group(@NotNull Stream<E> stream, @NotNull Function<E, K> keyMapper) {
        return Collections.unmodifiableMap(stream.collect(Collectors.groupingBy(keyMapper)));
    }

    /**
     * Maps some collection with map index
     * @param map map
     * @param function mapper
     * @return mapped stream
     * @param <K> key
     * @param <V> value
     * @param <R> return value
     */
    @NotNull
    public static <K, V, R> Stream<R> mapIndexed(@NotNull Map<K, V> map, @NotNull IndexedFunction<Map.Entry<K, V>, R> function) {
        return mapIndexed(map.entrySet(), function);
    }

    /**
     * Maps some collection with map index
     * @param collection collection
     * @param function mapper
     * @return mapped stream
     * @param <E> element
     * @param <R> return value
     */
    @NotNull
    public static <E, R> Stream<R> mapIndexed(@NotNull Collection<E> collection, @NotNull IndexedFunction<E, R> function) {
        return mapIndexed(collection.stream(), function);
    }

    /**
     * Maps some collection with map index
     * @param stream stream
     * @param function mapper
     * @return mapped stream
     * @param <E> element
     * @param <R> return value
     */
    @NotNull
    public static <E, R> Stream<R> mapIndexed(@NotNull Stream<E> stream, @NotNull IndexedFunction<E, R> function) {
        var integer = new AtomicInteger();
        return stream.map(e -> function.apply(integer.getAndIncrement(), e));
    }

    /**
     * Maps some stream to a float collection
     * @param stream stream
     * @param mapper mapper
     * @param creator float collection creator
     * @return float collection
     * @param <E> element
     * @param <T> type
     */
    @NotNull
    public static <E, T extends FloatCollection> T mapFloat(@NotNull Stream<E> stream, @NotNull FloatFunction<E> mapper, @NotNull Supplier<T> creator) {
        var collect = creator.get();
        stream.forEach(e -> collect.add(mapper.apply(e)));
        return collect;
    }

    /**
     * Map some map's value.
     * @param original original map
     * @param mapper value mapper
     * @return unmodifiable map
     * @param <K> key
     * @param <V> value
     * @param <R> new value
     */
    @NotNull
    @Unmodifiable
    public static <K, V, R> Map<K, R> mapValue(@NotNull Map<K, V> original, @NotNull Function<V, R> mapper) {
        return associate(original.entrySet(), Map.Entry::getKey, e -> mapper.apply(e.getValue()));
    }

    /**
     * Associates collection to map
     * @param collection collection
     * @param keyMapper key mapper
     * @param valueMapper value mapper
     * @return unmodifiable map
     * @param <E> element
     * @param <K> key
     * @param <V> value
     */
    @NotNull
    @Unmodifiable
    public static <E, K, V> Map<K, V> associate(@NotNull Collection<E> collection, @NotNull Function<E, K> keyMapper, @NotNull Function<E, V> valueMapper) {
        return associate(collection.stream(), keyMapper, valueMapper);
    }

    /**
     * Gets filter with warning if not matched
     * @param predicate delegated predicate
     * @param lazyLogFunction log function
     * @return predicate
     * @param <T> type
     */
    @NotNull
    public static <T> Predicate<T> filterWithWarning(@NotNull Predicate<T> predicate, @NotNull Function<T, String> lazyLogFunction) {
        var logger = BetterModel.plugin().logger();
        return t -> {
            var testedValue = predicate.test(t);
            if (!testedValue) logger.warn(LogUtil.toLog(lazyLogFunction.apply(t), NamedTextColor.YELLOW));
            return testedValue;
        };
    }

    /**
     * Associates stream to map
     * @param collection collection
     * @param keyMapper key mapper
     * @return unmodifiable map
     * @param <E> element
     * @param <K> key
     */
    @NotNull
    @Unmodifiable
    public static <E, K> Map<K, E> associate(@NotNull Collection<E> collection, @NotNull Function<E, K> keyMapper) {
        return associate(collection.stream(), keyMapper);
    }

    /**
     * Associates stream to map
     * @param stream stream
     * @param keyMapper key mapper
     * @return unmodifiable map
     * @param <E> element
     * @param <K> key
     */
    @NotNull
    @Unmodifiable
    public static <E, K> Map<K, E> associate(@NotNull Stream<E> stream, @NotNull Function<E, K> keyMapper) {
        return associate(stream, keyMapper, e -> e);
    }

    /**
     * Associates stream to map
     * @param stream stream
     * @param keyMapper key mapper
     * @param valueMapper value mapper
     * @return unmodifiable map
     * @param <E> element
     * @param <K> key
     * @param <V> value
     */
    @NotNull
    @Unmodifiable
    public static <E, K, V> Map<K, V> associate(@NotNull Stream<E> stream, @NotNull Function<E, K> keyMapper, @NotNull Function<E, V> valueMapper) {
        return stream.collect(Collectors.toUnmodifiableMap(keyMapper, valueMapper));
    }

    /**
     * Function with index
     * @param <T> type
     * @param <R> return value
     */
    @FunctionalInterface
    public interface IndexedFunction<T, R> {
        /**
         * Maps to new value
         * @param index index
         * @param t input
         * @return mapped value
         */
        R apply(int index, T t);
    }

    /**
     * Function of float
     * @param <T> type
     */
    @FunctionalInterface
    public interface FloatFunction<T> {
        /**
         * Maps to new value
         * @param t input
         * @return mapped float
         */
        float apply(T t);
    }
}
