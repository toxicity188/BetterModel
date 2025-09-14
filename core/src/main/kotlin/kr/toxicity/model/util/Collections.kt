package kr.toxicity.model.util

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream

fun <K, V> MutableMap<K, V>.toImmutableView(): Map<K, V> = Collections.unmodifiableMap(this)

fun <T> Stream<T>.toSet(): Set<T> = collect(Collectors.toUnmodifiableSet())
fun <T> Stream<T>.any(predicate: (T) -> Boolean): Boolean = anyMatch(predicate)

fun parallelIOThreadPool() = try {
    ParallelIOThreadPool()
} catch (error: OutOfMemoryError) {
    throw RuntimeException("You have to set your Linux max thread limit!", error)
}

class ParallelIOThreadPool : AutoCloseable {
    private val available = Runtime.getRuntime().availableProcessors() * 2
    private val pool = newFixedThreadPoolContext(available, "BetterModel-Worker-Dispatcher")

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
        runBlocking {
            tasks.map { async(pool) { it() } }.awaitAll()
        }
    }
}