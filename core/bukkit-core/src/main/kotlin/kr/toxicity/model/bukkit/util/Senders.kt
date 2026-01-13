package kr.toxicity.model.bukkit.util

import kr.toxicity.model.bukkit.BetterModelLibrary
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import org.bukkit.command.CommandSender

val ADVENTURE_PLATFORM = if (BetterModelLibrary.ADVENTURE_PLATFORM.isLoaded) BukkitAudiences.create(PLUGIN) else null

fun CommandSender.audience() = ADVENTURE_PLATFORM?.sender(this) ?: this
