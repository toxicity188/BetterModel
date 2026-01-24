/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.animation;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * A read-only storage for timed elements (keyframes), allowing indexed access.
 * <p>
 * This interface abstracts the underlying data structure (e.g., List, Array) used to store animation frames.
 * </p>
 *
 * @param <T> the type of timed element
 * @since 2.0.0
 */
public interface TimedStorage<T extends Timed> {

    /**
     * Creates a TimedStorage backed by a List.
     *
     * @param list the list of elements
     * @param <T> the type of element
     * @return a new TimedStorage
     * @since 2.0.0
     */
    @NotNull
    static <T extends Timed> TimedStorage<T> listOf(@NotNull List<T> list) {
        return new ListDelegate<>(list);
    }

    /**
     * Retrieves the element at the specified index.
     *
     * @param index the index of the element
     * @return the element
     * @throws IndexOutOfBoundsException if the index is out of range
     * @since 2.0.0
     */
    @NotNull T get(int index);

    /**
     * Returns the number of elements in the storage.
     *
     * @return the size
     * @since 2.0.0
     */
    int size();

    /**
     * Retrieves the last element in the storage.
     *
     * @return the last element
     * @throws java.util.NoSuchElementException if the storage is empty
     * @since 2.0.0
     */
    @NotNull T getLast();

    /**
     * A {@link TimedStorage} implementation that delegates to a {@link List}.
     *
     * @param list the backing list
     * @param <T> the type of element
     * @since 2.0.0
     */
    record ListDelegate<T extends Timed>(@NotNull List<T> list) implements TimedStorage<T> {
        @Override
        public @NonNull T get(int index) {
            return list.get(index);
        }

        @Override
        public int size() {
            return list.size();
        }

        @Override
        public @NonNull T getLast() {
            return list.getLast();
        }
    }
}
