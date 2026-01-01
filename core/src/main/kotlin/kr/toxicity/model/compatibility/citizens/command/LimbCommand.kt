/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
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
        usage = "limb <id> <model> <animation> <player> [loop_type]",
        desc = "",
        modifiers = ["limb"],
        min = 5,
        max = 6,
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
        @Arg(4) player: String,
        @Arg(5) type: String?
    ) {
        val targetNpc = CitizensAPI.getNPCRegistry().getById(id.toIntOrNull() ?: return) ?: return
        val npcEntity = targetNpc.entity as? Player ?: return
        val targetPlayer = Bukkit.getPlayer(player) ?: return

        val animType = type
            ?.let { t ->
                runCatching {
                    AnimationIterator.Type.valueOf(t.uppercase())
                }.getOrNull()
            }
            ?: AnimationIterator.Type.PLAY_ONCE

        BetterModel.limb(model)
            .map {
                it.getOrCreate(npcEntity, TrackerModifier.DEFAULT) { tracker ->
                    tracker.markPlayerForSpawn(targetPlayer)
                }
            }.ifPresent { tracker ->
                val success = tracker.animate(
                    animation,
                    AnimationModifier.builder()
                        .start(0)
                        .player(targetPlayer)
                        .type(animType)
                        .build()
                ) {
                    tracker.unmarkPlayerForSpawn(targetPlayer)
                    tracker.registry().remove(targetPlayer)
                    if (tracker.playerCount() == 0) tracker.close()
                }

                if (success) {
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
