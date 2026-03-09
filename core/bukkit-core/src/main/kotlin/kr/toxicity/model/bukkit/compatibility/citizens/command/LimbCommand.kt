/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.bukkit.compatibility.citizens.command

import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.animation.AnimationIterator
import kr.toxicity.model.api.animation.AnimationModifier
import kr.toxicity.model.api.tracker.TrackerModifier
import kr.toxicity.model.bukkit.util.wrap
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
        usage = "limb <id> <model> <animation> [loop_type] [player]",
        desc = "",
        modifiers = ["limb"],
        min = 4,
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
        @Arg(4) type: String?,
        @Arg(5) player: String?
    ) {
        val targetNpc = CitizensAPI.getNPCRegistry().getById(id.toIntOrNull() ?: return) ?: return
        val npcEntity = (targetNpc.entity as? Player)?.wrap() ?: return
        val targetPlayer = player?.let(Bukkit::getPlayer)?.wrap()

        val animType = type
            ?.let { value ->
                runCatching {
                    AnimationIterator.Type.valueOf(value.uppercase())
                }.getOrNull()
            }
            ?: AnimationIterator.Type.PLAY_ONCE

        BetterModel.limb(model)
            .map { renderer ->
                renderer.getOrCreate(npcEntity, TrackerModifier.DEFAULT) { tracker ->
                    if (targetPlayer != null) {
                        tracker.markPlayerForSpawn(targetPlayer)
                    }
                }
            }
            .ifPresent { tracker ->
                val success = tracker.animate(
                    animation,
                    AnimationModifier.builder()
                        .start(0)
                        .player(targetPlayer)
                        .type(animType)
                        .build()
                ) {
                    if (targetPlayer != null) {
                        tracker.unmarkPlayerForSpawn(targetPlayer)
                        tracker.registry().remove(targetPlayer)
                        if (tracker.playerCount() == 0) tracker.close()
                    } else {
                        tracker.close()
                    }
                }

                if (!success) {
                    tracker.close()
                    return@ifPresent
                }

                if (targetPlayer != null && !tracker.isSpawned(targetPlayer)) {
                    tracker.markPlayerForSpawn(targetPlayer)
                    tracker.registry().spawnIfNotSpawned(targetPlayer)
                }
            }
    }
}
