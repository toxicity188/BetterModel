/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.util.collection;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PriorityMapTest {
    @Test
    void ordersByDescendingPriorityThenLatestInsertion() {
        var map = new PriorityMap<Integer, String>();
        map.put(1, "minimum", Integer.MIN_VALUE);
        map.put(2, "old zero", 0);
        map.put(3, "maximum", Integer.MAX_VALUE);
        map.put(4, "new zero", 0);
        map.put(5, "negative", -1);
        assertEquals(List.of("maximum", "new zero", "old zero", "negative", "minimum"), values(map));
        assertEquals("old zero", map.put(2, "newest zero", 0));
        assertEquals(List.of("maximum", "newest zero", "new zero", "negative", "minimum"), values(map));
        assertEquals("maximum", map.put(3, "new minimum", Integer.MIN_VALUE));
        assertEquals(List.of("newest zero", "new zero", "negative", "new minimum", "minimum"), values(map));
    }

    @Test
    void returnsPreviousValuesAndKeepsReplacementPosition() {
        var map = new PriorityMap<Integer, String>();
        assertNull(map.put(1, "one", 0));
        map.put(2, "two", 0);
        assertEquals("one", map.replace(1, value -> value + " changed"));
        assertEquals("one changed", map.get(1));
        assertEquals(List.of("two", "one changed"), values(map));
        assertNull(map.replace(3, _ -> fail("Missing keys must not invoke the function")));
        assertEquals("one changed", map.remove(1));
        assertNull(map.remove(1));
        assertNull(map.get(1));
    }

    @Test
    void nullReplacementRemovesBothIndexes() throws ReflectiveOperationException {
        var map = new PriorityMap<Integer, String>();
        map.put(1, "one", 0);
        assertEquals("one", map.replace(1, _ -> null));
        assertTrue(map.isEmpty());
        var keyMap = PriorityMap.class.getDeclaredField("keyMap");
        keyMap.setAccessible(true);
        assertTrue(((Map<?, ?>) keyMap.get(map)).isEmpty());
        assertNull(map.get(1));
        assertNull(map.replace(1, _ -> fail("Removed key must not invoke the function")));
        assertNull(map.remove(1));
        assertNull(map.put(1, "again", 1));
        assertEquals(List.of("again"), values(map));
    }

    @Test
    void emptyIteratorHasNoNextValueOrRemovableElement() {
        var map = new PriorityMap<Integer, Integer>();
        var iterator = map.valueIterator();
        assertTrue(map.isEmpty());
        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, iterator::next);
        assertThrows(IllegalStateException.class, iterator::remove);
        map.put(1, 1, 0);
        var single = map.valueIterator();
        assertEquals(1, single.next());
        single.remove();
        assertFalse(single.hasNext());
        assertThrows(NoSuchElementException.class, single::next);
        assertThrows(IllegalStateException.class, single::remove);
        assertTrue(map.isEmpty());
    }

    @Test
    void iteratorRemovesFirstMiddleAndLastWithoutSkipping() {
        var map = new PriorityMap<Integer, Integer>();
        for (var i = 0; i < 6; i++) map.put(i, i, i);
        var iterator = map.valueIterator();
        assertThrows(IllegalStateException.class, iterator::remove);
        assertEquals(5, iterator.next());
        iterator.remove();
        assertThrows(IllegalStateException.class, iterator::remove);
        assertEquals(4, iterator.next());
        assertEquals(3, iterator.next());
        iterator.remove();
        assertEquals(2, iterator.next());
        assertEquals(1, iterator.next());
        assertEquals(0, iterator.next());
        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, iterator::next);
        iterator.remove();
        assertEquals(List.of(4, 2, 1), values(map));
        assertNull(map.get(5));
        assertNull(map.get(3));
        assertNull(map.get(0));
        iterator = map.valueIterator();
        while (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
        assertTrue(map.isEmpty());
        assertFalse(map.valueIterator().hasNext());
    }

    @Test
    void iteratorsAreIndependentAfterEarlyExitAndDuringNestedTraversal() {
        var map = new PriorityMap<Integer, Integer>();
        map.put(1, 1, 0);
        map.put(2, 2, 0);
        var outer = map.valueIterator();
        assertEquals(2, outer.next());
        assertEquals(List.of(2, 1), values(map));
        assertEquals(1, outer.next());
        outer.remove();
        assertEquals(List.of(2), values(map));
        assertFalse(outer.hasNext());
    }

    @Test
    void structuralChangesInvalidateOtherIteratorsButReplacementDoesNot() {
        var map = new PriorityMap<Integer, Integer>();
        map.put(1, 1, 0);
        map.put(2, 2, 0);
        var iterator = map.valueIterator();
        map.replace(2, _ -> 20);
        assertEquals(20, iterator.next());
        assertNull(map.remove(3));
        assertNull(map.replace(3, _ -> 3));
        assertEquals(1, iterator.next());

        iterator = map.valueIterator();
        iterator.next();
        map.put(2, 2, 0);
        assertThrows(ConcurrentModificationException.class, iterator::next);
        assertThrows(ConcurrentModificationException.class, iterator::remove);

        var first = map.valueIterator();
        var second = map.valueIterator();
        first.next();
        second.next();
        first.remove();
        assertThrows(ConcurrentModificationException.class, second::remove);
        assertThrows(ConcurrentModificationException.class, second::next);

        var third = map.valueIterator();
        map.replace(1, _ -> null);
        assertThrows(ConcurrentModificationException.class, third::next);
    }

    @Test
    void rejectsNullInputsBeforeMutationAndPropagatesCallbackFailures() {
        var map = new PriorityMap<Integer, String>();
        map.put(1, "one", 0);
        assertThrows(NullPointerException.class, () -> map.put(null, "null", 0));
        assertThrows(NullPointerException.class, () -> map.put(1, null, 0));
        assertThrows(NullPointerException.class, () -> map.get(null));
        assertThrows(NullPointerException.class, () -> map.remove(null));
        assertThrows(NullPointerException.class, () -> map.replace(null, value -> value));
        assertThrows(NullPointerException.class, () -> map.replace(1, null));
        var failure = new IllegalArgumentException("callback");
        assertSame(failure, assertThrows(IllegalArgumentException.class, () -> map.replace(1, _ -> {
            throw failure;
        })));
        assertEquals(List.of("one"), values(map));
        assertThrows(ConcurrentModificationException.class, () -> map.replace(1, _ -> {
            map.put(2, "two", 0);
            return "changed";
        }));
        assertEquals(List.of("two", "one"), values(map));
    }

    @Test
    void matchesReferenceAcrossRandomMutationsAndPartialIteratorRemoval() {
        var random = new Random(937);
        var map = new PriorityMap<Integer, String>();
        var reference = new HashMap<Integer, Expected>();
        for (var step = 0; step < 5_000; step++) {
            var key = random.nextInt(32);
            var value = "value " + step;
            var old = reference.get(key);
            switch (random.nextInt(6)) {
                case 0, 1 -> {
                    var priority = random.nextInt(8) == 0 ? random.nextInt() : 0;
                    assertEquals(old == null ? null : old.value, map.put(key, value, priority));
                    reference.put(key, new Expected(key, value, priority, step));
                }
                case 2 -> {
                    assertEquals(old == null ? null : old.value, map.remove(key));
                    reference.remove(key);
                }
                case 3 -> {
                    var remove = random.nextBoolean();
                    assertEquals(old == null ? null : old.value, map.replace(key, _ -> remove ? null : value));
                    if (old != null) {
                        if (remove) reference.remove(key);
                        else reference.put(key, new Expected(key, value, old.priority, old.insertion));
                    }
                }
                case 4 -> {
                    var expected = sorted(reference);
                    var iterator = map.valueIterator();
                    var visited = 0;
                    for (var entry : expected) {
                        if (visited++ == 5) break;
                        assertEquals(entry.value, iterator.next());
                        if (random.nextBoolean()) {
                            iterator.remove();
                            reference.remove(entry.key);
                        }
                    }
                }
                default -> assertEquals(old == null ? null : old.value, map.get(key));
            }
            assertEquals(reference.isEmpty(), map.isEmpty());
            assertEquals(sorted(reference).stream().map(Expected::value).toList(), values(map));
            for (var probe = 0; probe < 32; probe++) {
                var expected = reference.get(probe);
                assertEquals(expected == null ? null : expected.value, map.get(probe));
            }
        }
    }

    private record Expected(int key, String value, int priority, int insertion) {
    }

    private static List<Expected> sorted(Map<Integer, Expected> reference) {
        return reference.values().stream().sorted(Comparator.comparingInt(Expected::priority).reversed()
            .thenComparing(Comparator.comparingInt(Expected::insertion).reversed())).toList();
    }

    private static <K extends Comparable<K>, V> List<V> values(PriorityMap<K, V> map) {
        var result = new ArrayList<V>();
        map.valueIterator().forEachRemaining(result::add);
        return result;
    }
}
