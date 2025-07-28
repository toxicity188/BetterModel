package kr.toxicity.model.scheduler

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import kr.toxicity.model.api.scheduler.ModelScheduler
import kr.toxicity.model.api.scheduler.ModelTask
import kr.toxicity.model.util.PLUGIN
import org.bukkit.Bukkit
import org.bukkit.Location
import java.util.concurrent.TimeUnit

class PaperScheduler : ModelScheduler {

    private fun ScheduledTask.wrap() = object : ModelTask {
        override fun isCancelled(): Boolean = this@wrap.isCancelled
        override fun cancel() {
            this@wrap.cancel()
        }
    }

    private fun ifEnabled(block: () -> ModelTask?): ModelTask? {
        return if (PLUGIN.isEnabled) block() else null
    }

    override fun task(location: Location, runnable: Runnable): ModelTask? = ifEnabled {
        Bukkit.getRegionScheduler().run(PLUGIN, location) {
            runnable.run()
        }.wrap()
    }

    override fun taskLater(location: Location, delay: Long, runnable: Runnable): ModelTask? = ifEnabled {
        Bukkit.getRegionScheduler().runDelayed(PLUGIN, location, {
            runnable.run()
        }, delay).wrap()
    }

    override fun asyncTask(runnable: Runnable) = Bukkit.getAsyncScheduler().runNow(PLUGIN) {
        runnable.run()
    }.wrap()

    override fun asyncTaskLater(delay: Long, runnable: Runnable) = Bukkit.getAsyncScheduler().runDelayed(PLUGIN, {
        runnable.run()
    }, (delay * 50).coerceAtLeast(1), TimeUnit.MILLISECONDS).wrap()

    override fun asyncTaskTimer(delay: Long, period: Long, runnable: Runnable) = Bukkit.getAsyncScheduler().runAtFixedRate(PLUGIN, {
        runnable.run()
    }, (delay * 50).coerceAtLeast(1), (period * 50).coerceAtLeast(1), TimeUnit.MILLISECONDS).wrap()
}