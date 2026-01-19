/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.fabric.scheduler

import kr.toxicity.model.api.fabric.platform.FabricRegionHolder
import kr.toxicity.model.api.fabric.scheduler.FabricModelScheduler
import kr.toxicity.model.api.scheduler.ModelTask
import kr.toxicity.model.api.util.LogUtil
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class FabricModelSchedulerImpl : FabricModelScheduler, FabricRegionHolder {

    private val scheduler = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), object : ThreadFactory {

        private val integer = AtomicInteger()

        override fun newThread(r: Runnable): Thread {
            val thread = Thread(r)
            thread.setDaemon(true)
            thread.setName("BetterModel-Async-Scheduler-" + integer.getAndIncrement())
            thread.setUncaughtExceptionHandler { t: Thread, e: Throwable -> LogUtil.handleException("Exception has occurred in " + t.name, e) }
            return thread
        }
    })

    private val queue = ConcurrentLinkedQueue<SyncTask>()

    override fun asyncTask(runnable: Runnable): ModelTask {
        return scheduler.submit(runnable).wrap()
    }

    override fun asyncTaskLater(delay: Long, runnable: Runnable): ModelTask {
        return scheduler.schedule(runnable, delay * 50, TimeUnit.MILLISECONDS).wrap()
    }

    override fun asyncTaskTimer(delay: Long, period: Long, runnable: Runnable): ModelTask {
        return scheduler.scheduleAtFixedRate(runnable, delay * 50, period * 50, TimeUnit.MILLISECONDS).wrap()
    }

    override fun task(runnable: Runnable): ModelTask? {
        if (scheduler.isShutdown) return null
        return SyncTask(runnable).apply { queue += this }
    }

    override fun taskLater(delay: Long, runnable: Runnable): ModelTask? {
        if (scheduler.isShutdown) return null
        return SyncTask(runnable, delay).apply { queue += this }
    }

    private fun Future<*>.wrap() = object : ModelTask {
        override fun isCancelled(): Boolean = this@wrap.isCancelled
        override fun cancel() {
            cancel(true)
        }
    }

    private class SyncTask(
        @Volatile
        var task: Runnable,
        counter: Long = 0L
    ) : ModelTask {
        private val atomicCounter = AtomicLong(counter)

        fun run() = if (atomicCounter.getAndDecrement() <= 0) {
            synchronized(this) {
                task.run()
            }
            true
        } else false

        override fun isCancelled(): Boolean {
            return task === CANCELLED_TASK
        }

        override fun cancel() {
            if (isCancelled) return
            synchronized(this) {
                if (isCancelled) return
                task = CANCELLED_TASK
                atomicCounter.set(0)
            }
        }

        companion object {
            val CANCELLED_TASK: Runnable = {}
        }
    }

    fun tick() {
        queue.removeIf {
            it.run()
        }
    }

    fun shutdown() {
        scheduler.close()
    }
}
