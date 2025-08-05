package kr.toxicity.model.compatibility.citizens.command

import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.animation.AnimationIterator
import kr.toxicity.model.api.animation.AnimationModifier
import kr.toxicity.model.api.tracker.TrackerModifier
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.command.Arg
import net.citizensnpcs.api.command.Command
import net.citizensnpcs.api.command.CommandContext
import net.citizensnpcs.api.npc.NPC
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class LimbCommand {
    @Command(
        aliases = ["npc"],
        usage = "limb <id> <model> <animation> [player]",
        desc = "",
        modifiers = ["limb"],
        min = 5,
        max = 5,
        permission = "citizens.npc.animate"
    )
    @Suppress("UNUSED")
    fun animate(
        args: CommandContext,
        sender: CommandSender,
        npc: NPC?,
        @Arg(1) id: String,
        @Arg(2) model: String,
        @Arg(3) animation: String,
        @Arg(4) player: String
    ) {
        val targetNpc = CitizensAPI.getNPCRegistry().getById(id.toIntOrNull() ?: return) ?: return
        val npcEntity = targetNpc.entity as? Player ?: return
        val targetPlayer = Bukkit.getPlayer(player) ?: return
        BetterModel.limb(model)
            .map {
                it.getOrCreate(npcEntity, TrackerModifier.DEFAULT) { tracker ->
                    tracker.markPlayerForSpawn(targetPlayer)
                }
            }.ifPresent { tracker ->
                if (tracker.animate(
                        animation,
                        AnimationModifier.builder()
                            .start(0)
                            .player(targetPlayer)
                            .type(AnimationIterator.Type.PLAY_ONCE)
                            .build()
                    ) {
                        tracker.unmarkPlayerForSpawn(targetPlayer)
                        tracker.registry().remove(targetPlayer)
                        if (tracker.playerCount() == 0) tracker.close()
                    }
                ) {
                    if (!tracker.isSpawned(targetPlayer)) {
                        tracker.markPlayerForSpawn(targetPlayer)
                        tracker.registry().spawnIfNotSpawned(targetPlayer)
                    }
                } else {
                    tracker.close()
                }
            }
    }
}