/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.util

import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps
import it.unimi.dsi.fastutil.objects.Object2ObjectSortedMap
import it.unimi.dsi.fastutil.objects.Object2ObjectSortedMaps
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap
import it.unimi.dsi.fastutil.objects.Object2ReferenceMaps
import it.unimi.dsi.fastutil.objects.Object2ReferenceSortedMap
import it.unimi.dsi.fastutil.objects.Object2ReferenceSortedMaps
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps
import it.unimi.dsi.fastutil.objects.Reference2ObjectSortedMap
import it.unimi.dsi.fastutil.objects.Reference2ObjectSortedMaps
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMaps
import it.unimi.dsi.fastutil.objects.Reference2ReferenceSortedMap
import it.unimi.dsi.fastutil.objects.Reference2ReferenceSortedMaps
import kr.toxicity.model.api.util.CollectionUtil
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors
import java.util.stream.Stream

fun <K, V> addressingMapOf() = CollectionUtil.newAddressingMap<K, V>()
fun <K, V> sequencedAddressingMapOf() = CollectionUtil.newSequencedAddressingMap<K, V>()
fun <K, V> addressingMapOf(capacity: Int) = CollectionUtil.newAddressingMap<K, V>(capacity)
fun <K, V> sequencedAddressingMapOf(capacity: Int) = CollectionUtil.newSequencedAddressingMap<K, V>(capacity)

fun <K, V> emptySequencedMap(): SequencedMap<K, V> = Collections.emptyNavigableMap()

fun <K, V> MutableMap<K, V>.toImmutableView(): Map<K, V> = when (this) {
    is Object2ObjectMap<K, V> -> Object2ObjectMaps.unmodifiable(this)
    is Object2ReferenceMap<K, V> -> Object2ReferenceMaps.unmodifiable(this)
    is Reference2ObjectMap<K, V> -> Reference2ObjectMaps.unmodifiable(this)
    is Reference2ReferenceMap<K, V> -> Reference2ReferenceMaps.unmodifiable(this)
    else -> Collections.unmodifiableMap(this)
}

fun <K, V> SequencedMap<K, V>.toImmutableView(): SequencedMap<K, V> = when (this) {
    is Object2ObjectSortedMap<K, V> -> Object2ObjectSortedMaps.unmodifiable(this)
    is Object2ReferenceSortedMap<K, V> -> Object2ReferenceSortedMaps.unmodifiable(this)
    is Reference2ObjectSortedMap<K, V> -> Reference2ObjectSortedMaps.unmodifiable(this)
    is Reference2ReferenceSortedMap<K, V> -> Reference2ReferenceSortedMaps.unmodifiable(this)
    else -> Collections.unmodifiableSequencedMap(this)
}

fun <T> Stream<T>.toSet(): Set<T> = collect(Collectors.toUnmodifiableSet())
fun <T> Stream<T>.toMutableSet(): MutableSet<T> = collect(Collectors.toSet())

fun parallelIOThreadPool() = try {
    ParallelIOThreadPool()
} catch (error: OutOfMemoryError) {
    throw RuntimeException("You have to set your Linux max thread limit!", error)
}

class ParallelIOThreadPool : AutoCloseable {
    private val available = Runtime.getRuntime().availableProcessors() * 2
    private val integer = AtomicInteger()
    private val pool = Executors.newFixedThreadPool(available) {
        Thread(it).apply {
            isDaemon = true
            name = "BetterModel-IO-Worker-${integer.andIncrement}"
            uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { thread, exception ->
                exception.handleException("A error has been occurred in ${thread.name}")
            }
        }
    }

    override fun close() {
        pool.close()
    }

    fun <T> forEachParallel(list: List<T>, sizeAssume: (T) -> Long, block: (T) -> Unit) {
        if (list.isEmpty()) return
        val size = list.size
        val lastIndex = list.lastIndex
        val tasks = if (available >= size) {
            list.map {
                {
                    block(it)
                }
            }
        } else {
            val sorted = list.sortedBy(sizeAssume)
            val queue = arrayListOf<() -> Unit>()
            var i = 0
            val add = (size.toDouble() / available).toInt()
            while (i <= size) {
                val list = ArrayList<T>(add)
                for (t in i..<(i + add).coerceAtMost(size)) {
                    val ht = t / 2
                    list += sorted[if (t % 2 == 0) ht else lastIndex - ht]
                }
                queue += {
                    list.forEach(block)
                }
                i += add
            }
            queue
        }
        CompletableFuture.allOf(
            *tasks.map {
                CompletableFuture.runAsync({
                    it()
                }, pool)
            }.toTypedArray()
        ).join()
    }
}
