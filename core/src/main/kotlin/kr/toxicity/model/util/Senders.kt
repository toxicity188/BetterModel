package kr.toxicity.model.util

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.command.CommandSender

val INFO by lazy {
    Component.text()
        .content(" [!] ")
        .decorate(TextDecoration.BOLD)
        .color(NamedTextColor.GREEN)
        .build()
}
val WARN by lazy {
    Component.text()
        .content(" [!] ")
        .decorate(TextDecoration.BOLD)
        .color(NamedTextColor.RED)
        .build()
}

@Suppress("USELESS_IS_CHECK") //For legacy version and Spigot :(
fun CommandSender.audience() = if (this is Audience) this else PLUGIN.audiences().sender(this)
fun Audience.info(message: String) = info(Component.text(message))
fun Audience.warn(message: String) = warn(Component.text(message))
fun Audience.info(message: ComponentLike) = sendMessage(Component.text().append(INFO).append(message))
fun Audience.warn(message: ComponentLike) = sendMessage(Component.text().append(WARN).append(message))