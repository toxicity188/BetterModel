/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.bukkit.audience

import kr.toxicity.model.bukkit.util.audience
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

class AudiencePlayer(
    override val sender: Player
) : BukkitAudience {

    private val audience = sender.audience()

    override fun sendMessage(message: Component) {
        audience.sendMessage(message)
    }

    override fun showBossBar(bar: BossBar) {
        audience.showBossBar(bar)
    }

    override fun hideBossBar(bar: BossBar) {
        audience.hideBossBar(bar)
    }
}
