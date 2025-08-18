package kr.toxicity.model.manager.debug

import kr.toxicity.model.manager.ReloadPipeline
import kr.toxicity.model.util.componentOf
import kr.toxicity.model.util.emptyComponentOf
import kr.toxicity.model.util.withComma
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.format.NamedTextColor

class BossBarIndicator(
    private val audience: Audience
) : ReloadIndicator {
    private val bossBar = BossBar.bossBar(
        emptyComponentOf(),
        0F,
        BossBar.Color.GREEN,
        BossBar.Overlay.PROGRESS
    ).apply {
        audience.showBossBar(this)
    }

    override fun status(status: ReloadPipeline.Status) {
        bossBar.run {
            name(componentOf(status.status) {
                append(componentOf(" (${status.current.withComma()} / ${status.goal.withComma()})") {
                    color(NamedTextColor.YELLOW)
                })
            })
            progress(status.progress)
        }
    }

    override fun close() {
        audience.hideBossBar(bossBar)
    }
}