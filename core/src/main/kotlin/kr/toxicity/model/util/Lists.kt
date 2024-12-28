package kr.toxicity.model.util

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

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
            val queue = ArrayList<() -> Unit>()
            var i = 0
            val add = (size.toDouble() / available).toInt()
            while (i <= size) {
                val get = subList(i, (i + add).coerceAtMost(size))
                queue += {
                    get.forEach { t ->
                        block(t)
                    }
                }
                i += add
            }
            queue
        }
        try {
            Executors.newFixedThreadPool(tasks.size).use { pool ->
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