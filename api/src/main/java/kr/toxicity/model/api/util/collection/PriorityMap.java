/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.util.collection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

/**
 * A map that maintains the order of values based on priority, insertion order, and key comparison.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 * @since 3.0.0
 */
public final class PriorityMap<K extends Comparable<K>, V> {

    private final Map<K, Identifier<K>> keyMap = new HashMap<>();
    private final TreeMap<Identifier<K>, V> valueMap = new TreeMap<>();
    private long counter;

    /**
     * Returns true if this map contains no key-value mappings.
     *
     * @return true if this map contains no key-value mappings
     */
    public boolean isEmpty() {
        return valueMap.isEmpty();
    }

    /**
     * Internal identifier used to sort entries in the value map.
     */
    private record Identifier<K extends Comparable<K>>(
        int priority,
        long count,
        @NotNull K key
    ) implements Comparable<Identifier<K>> {

        /**
         * Compares this identifier with another based on priority (descending),
         * then insertion count (descending), and finally the key itself.
         *
         * @param o the object to be compared.
         * @return a negative integer, zero, or a positive integer as this object
         * is less than, equal to, or greater than the specified object.
         */
        @Override
        public int compareTo(@NotNull Identifier<K> o) {
            int c;
            if ((c = Integer.compare(o.priority, priority)) != 0) return c;
            if ((c = Long.compare(o.count, count)) != 0) return c;
            return key.compareTo(o.key);
        }
    }

    /**
     * Associates the specified value with the specified key and priority in this map.
     *
     * @param key      key with which the specified value is to be associated
     * @param value    value to be associated with the specified key
     * @param priority priority of the entry
     * @return the previous value associated with key, or null if there was no mapping for key.
     */
    public @Nullable V put(@NotNull K key, @NotNull V value, int priority) {
        return valueMap.put(
            keyMap.computeIfAbsent(Objects.requireNonNull(key), _ -> new Identifier<>(priority, counter++, key)),
            Objects.requireNonNull(value)
        );
    }

    /**
     * Returns the value to which the specified key is mapped, or null if this map contains no mapping for the key.
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or null if this map contains no mapping for the key
     */
    public @Nullable V get(@NotNull K key) {
        Identifier<K> identifier;
        return (identifier = keyMap.get(Objects.requireNonNull(key))) == null ? null : valueMap.get(identifier);
    }

    /**
     * Removes the mapping for a key from this map if it is present.
     *
     * @param key key whose mapping is to be removed from the map
     * @return the previous value associated with key, or null if there was no mapping for key.
     */
    public @Nullable V remove(@NotNull K key) {
        Identifier<K> identifier;
        return (identifier = keyMap.remove(Objects.requireNonNull(key))) == null ? null : valueMap.remove(identifier);
    }

    /**
     * Replaces the entry for the specified key only if it is currently mapped to some value.
     *
     * @param Key      key with which the specified value is associated
     * @param function the function to compute a value
     * @return the previous value associated with the specified key, or null if there was no mapping for the key.
     */
    public @Nullable V replace(@NotNull K Key, @NotNull Function<V, V> function) {
        Identifier<K> identifier;
        if ((identifier = keyMap.get(Objects.requireNonNull(Key))) == null) return null;
        return valueMap.computeIfPresent(identifier, (_, v) -> function.apply(v));
    }

    /**
     * Returns a Collection view of the values contained in this map, sorted by priority.
     *
     * @return a collection view of the values contained in this map
     */
    public @NotNull Collection<V> values() {
        return valueMap.values();
    }
}
