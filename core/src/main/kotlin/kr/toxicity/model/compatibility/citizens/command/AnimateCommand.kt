package kr.toxicity.model.compatibility.citizens.command

import kr.toxicity.model.api.animation.AnimationIterator
import kr.toxicity.model.api.animation.AnimationModifier
import kr.toxicity.model.api.tracker.EntityTrackerRegistry
import kr.toxicity.model.util.toSet
import net.citizensnpcs.api.command.Arg
import net.citizensnpcs.api.command.Arg.CompletionsProvider
import net.citizensnpcs.api.command.Command
import net.citizensnpcs.api.command.CommandContext
import net.citizensnpcs.api.command.CommandMessages
import net.citizensnpcs.api.npc.NPC
import org.bukkit.Bukkit
import org.bukkit.command.CommandException
import org.bukkit.command.CommandSender

class AnimateCommand {
    @Command(
        aliases = ["npc"],
        usage = "animate <animation> [speed] [player]",
        desc = "",
        modifiers = ["animate"],
        min = 2,
        max = 4,
        permission = "citizens.npc.animate"
    )
    @Suppress("UNUSED")
    fun animate(
        args: CommandContext,
        sender: CommandSender,
        npc: NPC?,
        @Arg(1, completionsProvider = TabComplete1::class) animation: String,
        @Arg(2, completionsProvider = TabComplete2::class) speed: String?,
        @Arg(3, completionsProvider = TabComplete3::class) player: String?
    ) {
        if (npc == null) throw CommandException(CommandMessages.MUST_HAVE_SELECTED)
        val spd = speed?.toFloatOrNull() ?: 1F
        val modifier = AnimationModifier.builder()
            .player(player?.let(Bukkit::getPlayer))
            .start(0)
            .end(0)
            .speed { spd }
            .type(AnimationIterator.Type.PLAY_ONCE)
            .build()
        EntityTrackerRegistry.registry(npc.entity.uniqueId)?.trackers()?.forEach {
            it.animate(animation, modifier)
        }
    }

    private class TabComplete1 : CompletionsProvider {
        override fun getCompletions(p0: CommandContext?, p1: CommandSender?, p2: NPC?): Collection<String> = p2?.entity?.let {
            EntityTrackerRegistry.registry(it.uniqueId)?.first()?.pipeline?.parent?.animations()
        } ?: emptySet()
    }
    private class TabComplete2 : CompletionsProvider {
        override fun getCompletions(p0: CommandContext?, p1: CommandSender?, p2: NPC?): Collection<String> = setOf("1.0")
    }
    private class TabComplete3 : CompletionsProvider {
        override fun getCompletions(p0: CommandContext?, p1: CommandSender?, p2: NPC?): Collection<String> = p2?.entity?.let {
            EntityTrackerRegistry.registry(it.uniqueId)?.viewedPlayer()?.map { channel ->
                channel.player().name
            }?.toSet()
        } ?: emptySet()
    }
}