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
 * A map that visits higher priorities first and the most recently inserted value first within a priority.
 * Replacing a value preserves its position; putting an existing key inserts it again.
 * Traversal starts in constant time; insertion can take linear time.
 * This map is not thread-safe. Its independent iterators support removal and are fail-fast
 * on a best-effort basis after structural changes outside that iterator.
 * <pre>{@code
 * var animations = new PriorityMap<String, String>();
 * animations.put("idle", "idle animation", 0);
 * var iterator = animations.valueIterator();
 * while (iterator.hasNext()) {
 *     if (iterator.next().isEmpty()) iterator.remove();
 * }
 * }</pre>
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 * @since 3.0.0
 */
public final class PriorityMap<K extends Comparable<K>, V> {

    private final Map<K, Node<K, V>> keyMap = new HashMap<>();
    private Node<K, V> first;
    private int modifications;

    /**
     * Returns true if this map contains no key-value mappings.
     *
     * @return true if this map contains no key-value mappings
     */
    public boolean isEmpty() {
        return first == null;
    }

    private static final class Node<K, V> {
        private final K key;
        private final int priority;
        private V value;
        private Node<K, V> previous;
        private Node<K, V> next;

        private Node(K key, V value, int priority) {
            this.key = key;
            this.value = value;
            this.priority = priority;
        }
    }

    /**
     * Associates the specified value with the specified key and priority in this map.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @param priority priority of the entry
     * @return the previous value associated with key, or null if there was no mapping for key
     * @throws NullPointerException if the key or value is null
     */
    public @Nullable V put(@NotNull K key, @NotNull V value, int priority) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        var node = new Node<>(key, value, priority);
        var old = keyMap.put(key, node);
        if (old != null) unlink(old);
        Node<K, V> previous = null;
        var next = first;
        while (next != null && next.priority > priority) {
            previous = next;
            next = next.next;
        }
        node.previous = previous;
        node.next = next;
        if (previous == null) first = node;
        else previous.next = node;
        if (next != null) next.previous = node;
        modifications++;
        return old == null ? null : old.value;
    }

    /**
     * Returns the value to which the specified key is mapped.
     *
     * @param key key whose associated value is to be returned
     * @return the mapped value, or null if this map contains no mapping for the key
     * @throws NullPointerException if the key is null
     */
    public @Nullable V get(@NotNull K key) {
        var node = keyMap.get(Objects.requireNonNull(key));
        return node == null ? null : node.value;
    }

    /**
     * Removes the mapping for a key from this map if it is present.
     *
     * @param key key whose mapping is to be removed from this map
     * @return the previous value, or null if there was no mapping for the key
     * @throws NullPointerException if the key is null
     */
    public @Nullable V remove(@NotNull K key) {
        var node = keyMap.remove(Objects.requireNonNull(key));
        if (node == null) return null;
        unlink(node);
        modifications++;
        return node.value;
    }

    private void unlink(Node<K, V> node) {
        if (node.previous == null) first = node.next;
        else node.previous.next = node.next;
        if (node.next != null) node.next.previous = node.previous;
    }

    /**
     * Replaces the entry for the specified key only if it is currently mapped to some value.
     * Preserves its priority and insertion order. A null result removes the mapping.
     *
     * @param key key with which the specified value is associated
     * @param function function to compute the value
     * @return the previous value, or null if there was no mapping for the key
     * @throws NullPointerException if the key is null, or the function is null for an existing key
     * @throws ConcurrentModificationException if the function structurally modifies this map
     */
    public @Nullable V replace(@NotNull K key, @NotNull Function<V, V> function) {
        var node = keyMap.get(Objects.requireNonNull(key));
        if (node == null) return null;
        var previous = node.value;
        var expected = modifications;
        var value = function.apply(previous);
        if (expected != modifications) throw new ConcurrentModificationException();
        if (value == null) remove(key);
        else node.value = value;
        return previous;
    }

    /**
     * Returns a new, independent iterator over the values in priority order.
     * The iterator supports {@link Iterator#remove()} and starts at the first value in constant time.
     *
     * @return a value iterator
     * @since 3.0.1
     */
    public @NotNull Iterator<V> valueIterator() {
        return new ValueIterator();
    }

    private final class ValueIterator implements Iterator<V> {
        private Node<K, V> next = first;
        private Node<K, V> last;
        private int expected = modifications;

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public V next() {
            if (next == null) throw new NoSuchElementException();
            if (expected != modifications) throw new ConcurrentModificationException();
            last = next;
            next = next.next;
            return last.value;
        }

        @Override
        public void remove() {
            if (last == null) throw new IllegalStateException();
            if (expected != modifications) throw new ConcurrentModificationException();
            keyMap.remove(last.key);
            unlink(last);
            expected = ++modifications;
            last = null;
        }
    }
}
