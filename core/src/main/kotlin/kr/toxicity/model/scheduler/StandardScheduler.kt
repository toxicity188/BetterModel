package kr.toxicity.model.scheduler

import kr.toxicity.model.api.scheduler.ModelScheduler
import kr.toxicity.model.api.scheduler.ModelTask
import kr.toxicity.model.util.PLUGIN
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.scheduler.BukkitTask

class StandardScheduler : ModelScheduler {

    private fun BukkitTask.wrap() = object : ModelTask {
        override fun isCancelled(): Boolean = this@wrap.isCancelled
        override fun cancel() {
            this@wrap.cancel()
        }
    }

    override fun task(location: Location, runnable: Runnable) = Bukkit.getScheduler().runTask(PLUGIN, runnable).wrap()
    override fun taskLater(delay: Long, location: Location, runnable: Runnable) = Bukkit.getScheduler().runTaskLater(PLUGIN, runnable, delay).wrap()
    override fun asyncTask(runnable: Runnable) = Bukkit.getScheduler().runTaskAsynchronously(PLUGIN, runnable).wrap()
    override fun asyncTaskLater(delay: Long, runnable: Runnable) = Bukkit.getScheduler().runTaskLaterAsynchronously(PLUGIN, runnable, delay).wrap()
    override fun asyncTaskTimer(delay: Long, period: Long, runnable: Runnable) = Bukkit.getScheduler().runTaskTimerAsynchronously(PLUGIN, runnable, delay, period).wrap()
}