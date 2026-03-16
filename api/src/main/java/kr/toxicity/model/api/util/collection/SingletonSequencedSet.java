/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.util.collection;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Unmodifiable
@ApiStatus.Internal
public final class SingletonSequencedSet<E> extends AbstractSet<E> implements SequencedSet<E> {

    private final E element;
    private final Set<E> delegate;

    public static <E> @NotNull SingletonSequencedSet<E> of(@NotNull E element) {
        return new SingletonSequencedSet<>(element);
    }

    private SingletonSequencedSet(@NotNull E element) {
        this.element = element;
        this.delegate = Collections.singleton(element);
    }

    public int size() {
        return 1;
    }

    public boolean contains(Object o) {
        return element.equals(o);
    }

    @Override
    public int hashCode() {
        return element.hashCode();
    }

    @NotNull
    public Iterator<E> iterator() {
        return delegate.iterator();
    }


    // Override default methods for Collection
    @Override
    @NotNull
    public Spliterator<E> spliterator() {
        return delegate.spliterator();
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        action.accept(element);
    }

    @Override
    public boolean removeIf(@NotNull Predicate<? super E> filter) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull Stream<E> stream() {
        return Stream.of(element);
    }

    @Override
    public @NotNull Stream<E> parallelStream() {
        return stream();
    }

    // Override default methods for SequencedCollection
    @Override
    public E getFirst() {
        return element;
    }

    @Override
    public E getLast() {
        return element;
    }

    @Override
    public E removeFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public E removeLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SequencedSet<E> reversed() {
        return this;
    }
}
