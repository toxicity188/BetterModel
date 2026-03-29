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

public final class PriorityMap<K extends Comparable<K>, V> {

    private final Map<K, Identifier<K>> keyMap = new HashMap<>();
    private final TreeMap<Identifier<K>, V> valueMap = new TreeMap<>();
    private long counter;

    public PriorityMap() {
    }

    private record Identifier<K extends Comparable<K>>(
        int priority,
        long count,
        @NotNull K key
    ) implements Comparable<Identifier<K>> {

        @Override
        public int compareTo(@NotNull Identifier<K> o) {
            int c;
            if ((c = Integer.compare(o.priority, priority)) != 0) return c;
            if ((c = Long.compare(o.count, count)) != 0) return c;
            return key.compareTo(o.key);
        }
    }

    public @Nullable V put(@NotNull K key, @NotNull V value, int priority) {
        return valueMap.put(
            keyMap.computeIfAbsent(Objects.requireNonNull(key), _ -> new Identifier<>(priority, counter++, key)),
            Objects.requireNonNull(value)
        );
    }

    public @Nullable V get(@NotNull K key) {
        Identifier<K> identifier;
        return (identifier = keyMap.get(Objects.requireNonNull(key))) == null ? null : valueMap.get(identifier);
    }

    public @Nullable V remove(@NotNull K key) {
        Identifier<K> identifier;
        return (identifier = keyMap.remove(Objects.requireNonNull(key))) == null ? null : valueMap.remove(identifier);
    }

    public @Nullable V replace(@NotNull K Key, @NotNull V newValue) {
        Identifier<K> identifier;
        if ((identifier = keyMap.get(Objects.requireNonNull(Key))) == null) return null;
        return valueMap.put(identifier, Objects.requireNonNull(newValue));
    }

    public @NotNull Collection<V> values() {
        return valueMap.values();
    }
}
