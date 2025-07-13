package kr.toxicity.model.util

import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

fun <K, V> MutableMap<K, V>.toImmutableView(): Map<K, V> = Collections.unmodifiableMap(this)

fun <T> List<T>.forEachAsync(block: (T) -> Unit) {
    if (isNotEmpty()) {
        val available = Runtime.getRuntime().availableProcessors()
            .coerceAtLeast(4)
            .coerceAtMost(256)
        val tasks = if (available >= size) {
            map {
                {
                    block(it)
                }
            }
        } else {
            val queue = arrayListOf<() -> Unit>()
            var i = 0
            val add = (size.toDouble() / available).toInt()
            while (i <= size) {
                val get = subList(i, (i + add).coerceAtMost(size))
                queue += {
                    get.forEach(block)
                }
                i += add
            }
            queue
        }
        try {
            val integer = AtomicInteger()
            Executors.newFixedThreadPool(tasks.size) {
                Thread(it).apply {
                    isDaemon = true
                    name = "BetterModel-Worker-${integer.andIncrement}"
                    uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { thread, exception ->
                        exception.handleException("A error has been occurred in ${thread.name}")
                    }
                }
            }.use { pool ->
                CompletableFuture.allOf(
                    *tasks.map {
                        CompletableFuture.runAsync({
                            it()
                        }, pool)
                    }.toTypedArray()
                ).join()
            }
        } catch (error: OutOfMemoryError) {
            warn(
                "Async task failed!",
                "You have to set your Linux max thread limit!",
                "",
                "Stack trace:",
                error.stackTraceToString()
            )
        }
    }
}