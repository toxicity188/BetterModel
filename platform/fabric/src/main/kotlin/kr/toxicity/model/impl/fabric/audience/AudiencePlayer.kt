/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.impl.fabric.audience

import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.level.ServerPlayer

data class AudiencePlayer(
    override val source: CommandSourceStack,
    val player: ServerPlayer
) : AudienceCommandSource {

    override fun sendMessage(message: Component) {
        player.sendMessage(message)
    }

    override fun showBossBar(bar: BossBar) {
        player.showBossBar(bar)
    }

    override fun hideBossBar(bar: BossBar) {
        player.hideBossBar(bar)
    }
}
