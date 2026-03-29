/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.test

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.animation.AnimationModifier
import kr.toxicity.model.api.fabric.platform.FabricPlayer
import kr.toxicity.model.api.tracker.ModelRotation
import kr.toxicity.model.api.tracker.TrackerModifier
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Input
import kotlin.jvm.optionals.getOrNull
import kotlin.math.atan2

class RollTest : ModInitializer {
    override fun onInitialize() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(argumentRoll())
        }
    }

    private fun argumentRoll(): LiteralArgumentBuilder<CommandSourceStack?>? {
        return Commands.literal("roll")
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(argumentInfo())
            .then(argumentPlay())
    }

    private fun argumentInfo(): LiteralArgumentBuilder<CommandSourceStack> {
        return Commands.literal("info")
            .executes { context ->
                executeInfo(context.source)
            }
    }

    private fun argumentPlay(): LiteralArgumentBuilder<CommandSourceStack> {
        return Commands.literal("play")
            .executes { context ->
                executePlay(context.source, context.source.playerOrException)
            }
    }

    private fun executeInfo(source: CommandSourceStack): Int {
        val renderer = BetterModel.limb("steve").getOrNull()
            ?: let {
                source.sendFailure(Component.literal("Renderer not found: steve"))
                return 0
            }

        val animation = renderer.animation("roll").getOrNull()
            ?: let {
                source.sendFailure(Component.literal("Animation not found: roll"))
                return 0
            }

        source.sendSuccess(
            {
                Component.empty()
                    .append("Loop mode: " + animation.loop)
                    .append("\n")
                    .append("Length: " + animation.length + " second")
            },
            true
        )
        return 1
    }

    private fun executePlay(source: CommandSourceStack, player: ServerPlayer): Int {
        val renderer = BetterModel.limb("steve").getOrNull()
            ?: let {
                source.sendFailure(Component.literal("Renderer not found: steve"))
                return 0
            }

        val yaw = player.lastClientInput.toYaw()
        val tracker = renderer.getOrCreate(
            FabricPlayer.of(player.connection),
            TrackerModifier.DEFAULT
        ) { tracker ->
            tracker.rotation {
                ModelRotation(
                    player.xRot,
                    (yaw + tracker.registry().entity().bodyYaw()).packDegrees()
                )
            }
        }

        val isAnimated = tracker.animate(
            "roll",
            AnimationModifier.DEFAULT_WITH_PLAY_ONCE
        ) {
            tracker.close()
        }

        if (!isAnimated) {
            tracker.close()
        }

        return 1
    }

    private fun Input.toYaw(): Float {
        val forward = (if (forward) 1 else 0) - (if (backward) 1 else 0)
        val right = (if (right) 1 else 0) - (if (left) 1 else 0)

        return if (forward == 0 && right == 0) {
            0f
        } else {
            Math.toDegrees(atan2(right.toDouble(), forward.toDouble())).toFloat()
        }
    }

    private fun Float.packDegrees(): Float {
        return if (this > 180) {
            this - 360
        } else {
            this
        }
    }
}
