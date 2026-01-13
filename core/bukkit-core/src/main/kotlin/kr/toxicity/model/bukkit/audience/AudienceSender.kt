package kr.toxicity.model.bukkit.audience

import kr.toxicity.model.bukkit.util.audience
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender

class AudienceSender(
    override val sender: CommandSender
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
