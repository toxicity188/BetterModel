package kr.toxicity.model.util

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.command.CommandSender

val INFO by lazy {
    componentOf(" [!] ") {
        decorate(TextDecoration.BOLD).color(NamedTextColor.GREEN)
    }
}
val WARN by lazy {
    componentOf(" [!] ") {
        decorate(TextDecoration.BOLD).color(NamedTextColor.RED)
    }
}

fun String.toComponent() = componentOf(this)

fun componentOf() = Component.text()
fun spaceComponentOf() = Component.space()
fun emptyComponentOf() = Component.empty()
fun lineComponentOf() = Component.newline()
fun componentOf(content: String) = componentOf().content(content)
fun componentOf(content: String, builder: TextComponent.Builder.() -> TextComponent.Builder) = componentOf {
    content(content).let(builder)
}
fun componentOf(builder: TextComponent.Builder.() -> TextComponent.Builder) = componentOf().let(builder).build()
fun componentOf(vararg like: ComponentLike) = componentOf {
    append(*like)
}
fun componentWithLineOf(vararg like: ComponentLike) = componentOf {
    like.forEachIndexed { i, l ->
        append(l)
        if (i < like.lastIndex) append(lineComponentOf())
    }
    this
}

@Suppress("USELESS_IS_CHECK") //For legacy version and Spigot :(
fun CommandSender.audience() = if (this is Audience) this else PLUGIN.audiences().sender(this)
fun Audience.info(message: String) = info(componentOf(message))
fun Audience.warn(message: String) = warn(componentOf(message))
fun Audience.info(vararg messages: ComponentLike) = sendMessage(componentWithLineOf(*messages.map { componentOf(INFO, it) }.toTypedArray()))
fun Audience.warn(vararg messages: ComponentLike) = sendMessage(componentWithLineOf(*messages.map { componentOf(WARN, it) }.toTypedArray()))
fun Audience.info(message: ComponentLike) = sendMessage(componentOf(INFO, message))
fun Audience.warn(message: ComponentLike) = sendMessage(componentOf(WARN, message))