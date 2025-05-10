package kr.toxicity.model.util

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender

val INFO by lazy {
    Component.text("[!] ").color(NamedTextColor.GREEN)
}
val WARN by lazy {
    Component.text("[!] ").color(NamedTextColor.RED)
}

fun CommandSender.audience() = PLUGIN.audiences().sender(this)
fun Audience.info(message: String) = info(Component.text(message))
fun Audience.warn(message: String) = warn(Component.text(message))
fun Audience.info(message: ComponentLike) = sendMessage(Component.text().append(INFO).append(message))
fun Audience.warn(message: ComponentLike) = sendMessage(Component.text().append(WARN).append(message))