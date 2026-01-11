package kr.toxicity.model.bukkit.util

import kr.toxicity.model.bukkit.BetterModelLibrary
import kr.toxicity.model.util.PLATFORM
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import org.bukkit.command.CommandSender

val ADVENTURE_PLATFORM = if (BetterModelLibrary.ADVENTURE_PLATFORM.isLoaded) BukkitAudiences.create(PLATFORM) else null

fun CommandSender.audience() = ADVENTURE_PLATFORM?.sender(this) ?: this
