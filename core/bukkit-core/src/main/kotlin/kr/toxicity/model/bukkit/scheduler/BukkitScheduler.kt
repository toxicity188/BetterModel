/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.bukkit.scheduler

import kr.toxicity.model.api.bukkit.scheduler.BukkitModelScheduler
import kr.toxicity.model.api.scheduler.ModelTask
import kr.toxicity.model.bukkit.util.PLUGIN
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.scheduler.BukkitTask

class BukkitScheduler : BukkitModelScheduler {

    private fun BukkitTask.wrap() = object : ModelTask {
        override fun isCancelled(): Boolean = this@wrap.isCancelled
        override fun cancel() {
            this@wrap.cancel()
        }
    }

    private fun ifEnabled(block: () -> ModelTask?): ModelTask? {
        return if (PLUGIN.isEnabled) block() else null
    }

    override fun task(location: Location, runnable: Runnable) = ifEnabled {
        Bukkit.getScheduler().runTask(PLUGIN, runnable).wrap()
    }
    override fun taskLater(location: Location, delay: Long, runnable: Runnable) = ifEnabled {
        Bukkit.getScheduler().runTaskLater(PLUGIN, runnable, delay).wrap()
    }
    override fun asyncTask(runnable: Runnable) = Bukkit.getScheduler().runTaskAsynchronously(PLUGIN, runnable).wrap()
    override fun asyncTaskLater(delay: Long, runnable: Runnable) = Bukkit.getScheduler().runTaskLaterAsynchronously(PLUGIN, runnable, delay).wrap()
    override fun asyncTaskTimer(delay: Long, period: Long, runnable: Runnable) = Bukkit.getScheduler().runTaskTimerAsynchronously(PLUGIN, runnable, delay, period).wrap()
}
