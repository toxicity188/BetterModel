/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.fabric.command

import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.BetterModelPlatform.ReloadResult.*
import kr.toxicity.model.api.animation.AnimationIterator
import kr.toxicity.model.api.animation.AnimationModifier
import kr.toxicity.model.api.fabric.platform.FabricLocation
import kr.toxicity.model.api.tracker.EntityHideOption
import kr.toxicity.model.api.tracker.ModelScaler
import kr.toxicity.model.api.tracker.Tracker
import kr.toxicity.model.api.tracker.TrackerModifier
import kr.toxicity.model.command.*
import kr.toxicity.model.fabric.audience.AudienceCommandSource
import kr.toxicity.model.fabric.audience.AudiencePlayer
import kr.toxicity.model.fabric.audience.AudienceSourceStack
import kr.toxicity.model.fabric.toRegistry
import kr.toxicity.model.fabric.toTracker
import kr.toxicity.model.fabric.wrap
import kr.toxicity.model.util.*
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.format.NamedTextColor.*
import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.registries.Registries
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.phys.Vec3
import org.incendo.cloud.SenderMapper
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.fabric.FabricServerCommandManager
import org.incendo.cloud.minecraft.modded.data.Coordinates
import org.incendo.cloud.minecraft.modded.data.MultipleEntitySelector
import org.incendo.cloud.minecraft.modded.data.SinglePlayerSelector
import org.incendo.cloud.minecraft.modded.parser.RegistryEntryParser.registryEntryParser
import org.incendo.cloud.minecraft.modded.parser.VanillaArgumentParsers.*
import org.incendo.cloud.parser.standard.BooleanParser.booleanParser
import org.incendo.cloud.parser.standard.DoubleParser.doubleParser
import org.incendo.cloud.parser.standard.EnumParser.enumParser
import org.incendo.cloud.parser.standard.StringParser.stringParser
import org.incendo.cloud.suggestion.SuggestionProvider.blockingStrings

private val MODEL_SUGGESTION = blockingStrings<Audience> { _, _ -> BetterModel.modelKeys() }
private val LIMB_SUGGESTION = blockingStrings<Audience> { _, _ -> BetterModel.limbKeys() }

fun startFabricCommand() {
    command(
        FabricServerCommandManager(
            ExecutionCoordinator.simpleCoordinator(),
            SenderMapper.create<CommandSourceStack, Audience>(
                { stack -> stack.player?.let { player -> AudiencePlayer(stack, player) } ?: AudienceSourceStack(stack) },
                { audience -> (audience as AudienceCommandSource).source }
            )
        ),
        "bettermodel",
        "All-related command.",
        "bm", "model"
    ) {
        create(
            "reload",
            "Reloads BetterModel.",
            "re", "rl"
        ) {
            handler(::reload)
        }
        create(
            "spawn",
            "Summons some model to given type",
            "s"
        ) {
            required("model", stringParser(), MODEL_SUGGESTION)
                .optional("type", registryEntryParser(Registries.ENTITY_TYPE, EntityType::class.java))
                .optional("scale", doubleParser(0.0625, 16.0))
                .optional("location", vec3Parser(true))
                .senderType(AudiencePlayer::class.java)
                .handler(::spawn)
        }
        create(
            "test",
            "Tests some model's animation to specific source",
            "t"
        ) {
            required("model", stringParser(), MODEL_SUGGESTION)
                .required(
                    "animation",
                    stringParser(),
                    blockingStrings { ctx, _ -> ctx.nullableString("model") { BetterModel.modelOrNull(it)?.animations()?.keys } ?: emptySet()  }
                )
                .optional("source", singlePlayerSelectorParser())
                .optional("location", vec3Parser(false))
                .handler(::test)
        }
        create(
            "disguise",
            "Disguises self.",
            "d"
        ) {
            required("model", stringParser(), MODEL_SUGGESTION)
                .optional("scaling", booleanParser())
                .senderType(AudiencePlayer::class.java)
                .handler(::disguise)
        }
        create(
            "undisguise",
            "Undisguises self.",
            "ud"
        ) {
            senderType(AudiencePlayer::class.java)
                .optional("model", stringParser(), blockingStrings { ctx, _ -> ctx.sender().player.toRegistry()?.trackers()?.map(Tracker::name) ?: emptyList() })
                .handler(::undisguise)
        }
        create(
            "play",
            "Plays source animation",
            "p"
        ) {
            required("limb", stringParser(), LIMB_SUGGESTION)
                .required(
                    "animation",
                    stringParser(),
                    blockingStrings { ctx, _ -> ctx.nullableString("limb") { BetterModel.limbOrNull(it)?.animations()?.keys } ?: emptySet()  }
                )
                .optional("loop_type", enumParser(AnimationIterator.Type::class.java))
                .optional("hide", booleanParser())
                .senderType(AudiencePlayer::class.java)
                .handler(::play)
        }
// TODO NOT implemented yet
//        create(
//            "hide",
//            "Hides some entities from target source."
//        ) {
//            required("model", stringParser(), MODEL_SUGGESTION)
//                .required("source", singlePlayerSelectorParser())
//                .required("entities", multipleEntitySelectorParser())
//                .handler(::hide)
//        }
//        create(
//            "show",
//            "Shows some entities to target source."
//        ) {
//            required("model", stringParser(), MODEL_SUGGESTION)
//                .required("source", singlePlayerSelectorParser())
//                .required("entities", multipleEntitySelectorParser())
//                .handler(::show)
//        }
//        create(
//            "version",
//            "Checks BetterModel's version",
//            "v"
//        ) {
//            handler(::version)
//        }
    }
}

private fun hide(context: CommandContext<Audience>) {
    val sender = context.sender()
    val model = context.get<String>("model")
    val player = context.get<SinglePlayerSelector>("source").single().connection.wrap()
    var success = false
    context.get<MultipleEntitySelector>("entities").values().forEach {
        if (it.toRegistry()?.tracker(model)?.hide(player) == true) success = true
    }
    if (!success) sender.warn("Failed to hide any of provided entities.")
}

private fun show(context: CommandContext<Audience>) {
    val sender = context.sender()
    val model = context.get<String>("model")
    val player = context.get<SinglePlayerSelector>("source").single().connection.wrap()
    var success = false
    context.get<MultipleEntitySelector>("entities").values().forEach {
        if (it.toRegistry()?.tracker(model)?.show(player) == true) success = true
    }
    if (!success) sender.warn("Failed to show any of provided entities.")
}

private fun disguise(context: CommandContext<AudiencePlayer>) {
    val audience = context.sender()
    val player = audience.player
    val scaling = if (context.getOrDefault("scaling", true)) ModelScaler.entity() else ModelScaler.defaultScaler()
    context.model("model") { return audience.warn("Unable to find this model: $it") }.getOrCreate(player.connection.wrap(), TrackerModifier.DEFAULT) {
        it.scaler(scaling)
    }
}

private fun undisguise(context: CommandContext<AudiencePlayer>) {
    val audience = context.sender()
    val player = audience.player
    val model = context.nullable<String>("model")
    if (model != null) {
        player.toTracker(model)?.close() ?: audience.warn("Cannot find this model to undisguise: $model")
    } else player.toRegistry()?.close() ?: audience.warn("Cannot find any model to undisguise")
}

private fun spawn(context: CommandContext<AudiencePlayer>) {
    val audience = context.sender()
    val player = audience.player
    val model = context.model("model") { return audience.warn("Unable to find this model: $it") }
    val type = context.nullable<EntityType<*>>("type", EntityType.HUSK)
    val scale = context.nullable("scale", 1.0)
    val loc = context.nullable<Coordinates>("location")
    type.spawn(
        player.level(),
        loc?.blockPos() ?: player.blockPosition(),
        EntitySpawnReason.COMMAND
    )?.let { entity ->
        model.create(entity.wrap(), TrackerModifier.DEFAULT) { tracker -> tracker.scaler(ModelScaler.entity().multiply(scale.toFloat())) }
    } ?: audience.warn("Entity spawning has been blocked.")
}

private fun version(context: CommandContext<Audience>) {
    val sender = context.sender()
    sender.info("Searching version, please wait...")
    PLATFORM.scheduler().asyncTask {
        val version = LATEST_VERSION
        sender.infoNotNull(
            emptyComponentOf(),
            "Current: ${PLATFORM.semver()}".toComponent(),
            version.release?.let { version -> componentOf("Latest release: ") { append(version.toURLComponent()) } },
            version.snapshot?.let { version -> componentOf("Latest snapshot: ") { append(version.toURLComponent()) } }
        )
    }
}

private fun reload(context: CommandContext<Audience>) {
    val audience = context.sender()
    PLATFORM.scheduler().asyncTask {
        audience.info("Start reloading. please wait...")
        when (val result = PLATFORM.reload(audience)) {
            is OnReload -> audience.warn("BetterModel is still on reload!")
            is Success -> {
                audience.info(
                    emptyComponentOf(),
                    "Reload completed. (${result.totalTime().withComma()}ms)".toComponent(GREEN),
                    "Assets reload time - ${result.assetsTime().withComma()}ms".toComponent {
                        color(GRAY)
                        hoverEvent("Reading all config and model.".toComponent().toHoverEvent())
                    },
                    "Packing time - ${result.packingTime().withComma()}ms".toComponent {
                        color(GRAY)
                        hoverEvent("Packing all model to resource pack.".toComponent().toHoverEvent())
                    },
                    "${BetterModel.models().size.withComma()} of models are loaded successfully. (${result.length().toByteFormat()})".toComponent(YELLOW),
                    (if (result.packResult.changed()) "${result.packResult.size().withComma()} of files are zipped." else "Zipping is skipped due to the same result.").toComponent(YELLOW),
                    emptyComponentOf()
                )
            }
            is Failure -> {
                audience.warn(
                    emptyComponentOf(),
                    "Reload failed.".toComponent(),
                    "Please read the log to find the problem.".toComponent(),
                    emptyComponentOf()
                )
                audience.warn()
                result.throwable.handleException("Reload failed.")
            }
        }
    }
}

private fun play(context: CommandContext<AudiencePlayer>) {
    val audience = context.sender()
    val player = audience.player
    val limb = context.limb("limb") { return audience.warn("Unable to find this limb: $it") }
    val animation = context.string("animation") { limb.animation(it).orElse(null) ?: return audience.warn("Unable to find this animation: $it") }
    val loopType = context.nullable("loop_type", AnimationIterator.Type.PLAY_ONCE)
    val hide = context.nullable<Boolean>("hide") != false
    limb.getOrCreate(player.connection.wrap(), TrackerModifier.DEFAULT) {
        it.hideOption(if (hide) EntityHideOption.DEFAULT else EntityHideOption.FALSE)
    }.run {
        if (!animate(animation, AnimationModifier(0, 0, loopType), ::close)) close()
    }
}

private fun test(context: CommandContext<Audience>) {
    val audience = context.sender()
    val model = context.model("model") { return audience.warn("Unable to find this model: $it") }
    val animation = context.string("animation") { str -> model.animation(str).orElse(null) ?: return audience.warn("Unable to find this animation: $str") }
    val player = context.nullable<SinglePlayerSelector>("source")?.single() ?: (audience as? AudiencePlayer)?.player ?: return audience.warn("Unable to find target source.")
    val location = context.nullable<Coordinates>("location")?.position() ?: player.position()
        .add(Vec3(0.0, 0.0, 10.0).yRot(-Math.toRadians(player.yRot.toDouble()).toFloat()))

    model.create(FabricLocation(
        player.level(),
        location.x,
        location.y,
        location.z,
        player.xRot,
        player.yRot + 180
    )).run {
        spawn(player.connection.wrap())
        animate(animation, AnimationModifier(0, 0, AnimationIterator.Type.PLAY_ONCE), ::close)
    }
}
