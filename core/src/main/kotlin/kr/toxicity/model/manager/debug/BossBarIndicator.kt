/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.manager.debug

import kr.toxicity.model.manager.ReloadPipeline
import kr.toxicity.model.util.componentOf
import kr.toxicity.model.util.emptyComponentOf
import kr.toxicity.model.util.toComponent
import kr.toxicity.model.util.withComma
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.format.NamedTextColor

class BossBarIndicator(
    private val audience: Audience
) : ReloadIndicator {

    private var showed = false
    private val bossBar by lazy {
        BossBar.bossBar(
            emptyComponentOf(),
            0F,
            BossBar.Color.GREEN,
            BossBar.Overlay.PROGRESS
        ).apply {
            showed = true
            audience.showBossBar(this)
        }
    }

    override fun status(status: ReloadPipeline.Status) {
        bossBar.run {
            name(componentOf(status.status) {
                append(" (${status.current.withComma()} / ${status.goal.withComma()})".toComponent(NamedTextColor.YELLOW))
            })
            progress(status.progress)
        }
    }

    override fun close() {
        if (showed) audience.hideBossBar(bossBar)
    }
}