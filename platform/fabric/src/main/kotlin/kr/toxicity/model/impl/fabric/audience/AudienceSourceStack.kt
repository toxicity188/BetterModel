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

data class AudienceSourceStack(override val source: CommandSourceStack) : AudienceCommandSource {
    override fun sendMessage(message: Component) {
        source.sendMessage(message)
    }

    override fun showBossBar(bar: BossBar) {
        source.showBossBar(bar)
    }

    override fun hideBossBar(bar: BossBar) {
        source.hideBossBar(bar)
    }
}
