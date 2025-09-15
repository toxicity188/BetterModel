/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.compatibility.citizens.command

import kr.toxicity.model.api.animation.AnimationIterator
import kr.toxicity.model.api.animation.AnimationModifier
import kr.toxicity.model.api.tracker.EntityTrackerRegistry
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.command.Arg
import net.citizensnpcs.api.command.Command
import net.citizensnpcs.api.command.CommandContext
import net.citizensnpcs.api.npc.NPC
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender

class AnimateCommand {
    @Command(
        aliases = ["npc"],
        usage = "animate <id> <animation> [speed] [player]",
        desc = "",
        modifiers = ["animate"],
        min = 3,
        max = 5,
        permission = "citizens.npc.animate"
    )
    @Suppress("UNUSED")
    fun animate(
        args: CommandContext,
        sender: CommandSender,
        npc: NPC?,
        @Arg(1) id: String,
        @Arg(2) animation: String,
        @Arg(3) speed: String?,
        @Arg(4) player: String?
    ) {
        val targetNpc = CitizensAPI.getNPCRegistry().getById(id.toIntOrNull() ?: return) ?: return
        val spd = speed?.toFloatOrNull() ?: 1F
        val modifier = AnimationModifier.builder()
            .player(player?.let(Bukkit::getPlayer))
            .start(0)
            .end(0)
            .speed { spd }
            .type(AnimationIterator.Type.PLAY_ONCE)
            .build()
        EntityTrackerRegistry.registry(targetNpc.entity.uniqueId)?.trackers()?.forEach {
            it.animate(animation, modifier)
        }
    }
}