package kr.toxicity.model.manager

import kr.toxicity.model.manager.debug.ReloadIndicator
import kr.toxicity.model.util.PLUGIN
import kr.toxicity.model.util.parallelIOThreadPool
import java.util.concurrent.atomic.AtomicInteger

class ReloadPipeline(
    private val indicators: List<ReloadIndicator>
) : AutoCloseable {

    var status = "Starting..."
    private val pool = parallelIOThreadPool()

    private val current = AtomicInteger()
    private var goal = 0

    infix fun goal(int: Int) {
        goal = int
        current.set(0)
    }

    fun progress() {
        current.incrementAndGet()
    }

    fun <T> forEachParallel(list: List<T>, sizeAssume: (T) -> Long, block: (T) -> Unit) {
        pool.forEachParallel(list, sizeAssume, block)
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