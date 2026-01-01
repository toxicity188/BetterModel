/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.manager

import kr.toxicity.model.manager.debug.ReloadIndicator
import kr.toxicity.model.util.PLUGIN
import kr.toxicity.model.util.parallelIOThreadPool
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class ReloadPipeline(
    private val indicators: List<ReloadIndicator>
) : AutoCloseable {

    var status = "Starting..."
    private val pool = parallelIOThreadPool()

    private val current = AtomicInteger()
    var goal = 0
        set(value) {
            field = value
            current.set(0)
        }

    fun progress() = current.incrementAndGet()

    fun <T> forEachParallel(list: List<T>, sizeAssume: (T) -> Long, block: (T) -> Unit) {
        pool.forEachParallel(list, sizeAssume, block)
    }

    fun <T, R> mapParallel(list: List<T>, sizeAssume: (T) -> Long, block: (T) -> R?): List<R> {
        return CopyOnWriteArrayList<R>().apply {
            forEachParallel(list, sizeAssume) { t: T ->
                block(t)?.let { add(it) }
            }
        }
    }

    private val task = PLUGIN.scheduler().asyncTaskTimer(1, 1) {
        current.get().run {
            Status(
                if (goal > 0) toFloat() / goal.toFloat() else 0F,
                this,
                goal,
                status
            )
        }.run {
            indicators.forEach {
                it status this
            }
        }
    }

    data class Status(
        val progress: Float,
        val current: Int,
        val goal: Int,
        val status: String
    )

    override fun close() {
        task.cancel()
        indicators.forEach(ReloadIndicator::close)
        pool.close()
    }
}