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
import kr.toxicity.model.fabric.logger
import kr.toxicity.model.fabric.modId
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class FabricModelSchedulerImpl :
    FabricModelScheduler,
    FabricRegionHolder
{
    private val idGenerator = AtomicInteger(1)

    private var currentTick = 0L

    private val taskQueue = PriorityQueue<FabricTaskImpl>()
    private val taskMap = ConcurrentHashMap<Int, FabricTaskImpl>()

    private val asyncExecutor: ExecutorService = Executors.newCachedThreadPool { runnable ->
        Thread(
            runnable,
            "${modId()}-scheduler"
        )
    }

    fun tick() {
        val tasks = mutableListOf<FabricTaskImpl>()
        synchronized(this) {
            currentTick++
            while (true) {
                val task = taskQueue.peek() ?: break
                if (task.nextRun > currentTick) {
                    break
                }

                taskQueue.poll()
                if (!taskMap.containsKey(task.id)) {
                    continue
                }

                tasks.add(task)
                if (task.period > 0) {
                    task.nextRun = currentTick + task.period
                    if (taskMap.containsKey(task.id)) {
                        taskQueue.add(task)
                    }
                } else {
                    taskMap.remove(task.id)
                }
            }
        }

        for (task in tasks) {
            if (task.isCancelled()) {
                continue
            }

            if (task.isSync) {
                safeRun(task)
            } else {
                asyncExecutor.submit {
                    if (!task.isCancelled()) {
                        safeRun(task)
                    }
                }
            }
        }
    }

    fun shutdown() {
        asyncExecutor.shutdown()
        synchronized(this) {
            taskQueue.clear()
            taskMap.clear()
        }
    }

    private fun safeRun(task: FabricTaskImpl) {
        try {
            task.run()
        } catch (_: Throwable) {
            logger().error("Failed to run task: id={}", task.id)
        }
    }

    override fun task(runnable: Runnable): ModelTask {
        return schedule(
            0,
            -1,
            true
        ) {
            runnable.run()
        }
    }

    override fun taskLater(delay: Long, runnable: Runnable): ModelTask {
        return schedule(
            delay,
            -1,
            true
        ) {
            runnable.run()
        }
    }

    override fun asyncTask(runnable: Runnable): ModelTask {
        return schedule(
            0,
            -1,
            false
        ) {
            runnable.run()
        }
    }

    override fun asyncTaskLater(delay: Long, runnable: Runnable): ModelTask {
        return schedule(
            delay,
            -1,
            false
        ) {
            runnable.run()
        }
    }

    override fun asyncTaskTimer(delay: Long, period: Long, runnable: Runnable): ModelTask {
        return schedule(
            delay,
            period,
            false
        ) {
            runnable.run()
        }
    }

    private fun schedule(delay: Long, period: Long, isSync: Boolean, action: () -> Unit): ModelTask {
        val safeDelay = if (delay < 0) 0 else delay
        val safePeriod = if (period == 0L) 1L else if (period < -1L) -1L else period

        val id = idGenerator.getAndIncrement()

        var scheduledTick: Long
        synchronized(this) {
            scheduledTick = currentTick
        }

        val task = FabricTaskImpl(
            id = id,
            isSync = isSync,
            period = safePeriod,
            initialRun = scheduledTick + safeDelay,
            action = action,
            removeAction = { taskMap.remove(id) }
        )

        synchronized(this) {
            taskMap[id] = task
            taskQueue.add(task)
        }

        return task
    }

    private class FabricTaskImpl(
        val id: Int,
        val isSync: Boolean,
        val period: Long,
        initialRun: Long,
        private val action: () -> Unit,
        private val removeAction: () -> Unit
    ) :
        ModelTask,
        Comparable<FabricTaskImpl>
    {
        @Volatile
        var nextRun: Long = initialRun

        @Volatile
        private var cancelled = false

        fun run() = action()

        override fun isCancelled(): Boolean = cancelled

        override fun cancel() {
            cancelled = true
            removeAction()
        }

        override fun compareTo(other: FabricTaskImpl): Int {
            val runDiff = nextRun.compareTo(other.nextRun)
            return if (runDiff == 0) {
                id.compareTo(other.id)
            } else {
                runDiff
            }
        }
    }
}
