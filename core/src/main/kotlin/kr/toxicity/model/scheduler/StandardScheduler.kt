package kr.toxicity.model.scheduler

import kr.toxicity.model.api.scheduler.ModelScheduler
import kr.toxicity.model.api.scheduler.ModelTask
import kr.toxicity.model.util.PLUGIN
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.scheduler.BukkitTask

class StandardScheduler : ModelScheduler {

    private fun BukkitTask.wrap() = object : ModelTask {
        override fun isCancelled(): Boolean = this@wrap.isCancelled
        override fun cancel() {
            this@wrap.cancel()
        }
    }

    private fun ifEnabled(block: () -> ModelTask?): ModelTask? {
        return if (PLUGIN.isEnabled) block() else null
    }

    override fun task(entity: Entity, runnable: Runnable) = ifEnabled {
        Bukkit.getScheduler().runTask(PLUGIN, runnable).wrap()
    }
    override fun taskLater(delay: Long, entity: Entity, runnable: Runnable) = ifEnabled {
        Bukkit.getScheduler().runTaskLater(PLUGIN, runnable, delay).wrap()
    }
    override fun asyncTask(runnable: Runnable) = Bukkit.getScheduler().runTaskAsynchronously(PLUGIN, runnable).wrap()
    override fun asyncTaskLater(delay: Long, runnable: Runnable) = Bukkit.getScheduler().runTaskLaterAsynchronously(PLUGIN, runnable, delay).wrap()
    override fun asyncTaskTimer(delay: Long, period: Long, runnable: Runnable) = Bukkit.getScheduler().runTaskTimerAsynchronously(PLUGIN, runnable, delay, period).wrap()
}