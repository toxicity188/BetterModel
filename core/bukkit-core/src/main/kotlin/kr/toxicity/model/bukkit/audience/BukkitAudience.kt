package kr.toxicity.model.bukkit.audience

import net.kyori.adventure.audience.Audience
import org.bukkit.command.CommandSender

interface BukkitAudience : Audience {
    val sender: CommandSender
}
