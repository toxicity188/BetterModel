/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.bukkit.command

import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.BetterModelPlatform.ReloadResult.Failure
import kr.toxicity.model.api.BetterModelPlatform.ReloadResult.OnReload
import kr.toxicity.model.api.BetterModelPlatform.ReloadResult.Success
import kr.toxicity.model.api.animation.AnimationIterator
import kr.toxicity.model.api.animation.AnimationModifier
import kr.toxicity.model.api.tracker.EntityHideOption
import kr.toxicity.model.api.tracker.ModelScaler
import kr.toxicity.model.api.tracker.Tracker
import kr.toxicity.model.api.tracker.TrackerModifier
import kr.toxicity.model.bukkit.audience.AudiencePlayer
import kr.toxicity.model.bukkit.audience.AudienceSender
import kr.toxicity.model.bukkit.audience.BukkitAudience
import kr.toxicity.model.bukkit.util.PLUGIN
import kr.toxicity.model.bukkit.util.toRegistry
import kr.toxicity.model.bukkit.util.toTracker
import kr.toxicity.model.bukkit.util.wrap
import kr.toxicity.model.command.command
import kr.toxicity.model.command.limb
import kr.toxicity.model.command.model
import kr.toxicity.model.command.nullable
import kr.toxicity.model.command.nullableString
import kr.toxicity.model.command.string
import kr.toxicity.model.util.LATEST_VERSION
import kr.toxicity.model.util.PLATFORM
import kr.toxicity.model.util.componentOf
import kr.toxicity.model.util.emptyComponentOf
import kr.toxicity.model.util.handleException
import kr.toxicity.model.util.info
import kr.toxicity.model.util.infoNotNull
import kr.toxicity.model.util.toByteFormat
import kr.toxicity.model.util.toComponent
import kr.toxicity.model.util.toHoverEvent
import kr.toxicity.model.util.warn
import kr.toxicity.model.util.withComma
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.format.NamedTextColor.GRAY
import net.kyori.adventure.text.format.NamedTextColor.GREEN
import net.kyori.adventure.text.format.NamedTextColor.YELLOW
import org.bukkit.command.CommandSender
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.incendo.cloud.SenderMapper
import org.incendo.cloud.bukkit.CloudBukkitCapabilities
import org.incendo.cloud.bukkit.data.MultipleEntitySelector
import org.incendo.cloud.bukkit.parser.PlayerParser.playerParser
import org.incendo.cloud.bukkit.parser.location.LocationParser.locationParser
import org.incendo.cloud.bukkit.parser.selector.MultipleEntitySelectorParser.multipleEntitySelectorParser
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.paper.LegacyPaperCommandManager
import org.incendo.cloud.parser.standard.BooleanParser.booleanParser
import org.incendo.cloud.parser.standard.DoubleParser.doubleParser
import org.incendo.cloud.parser.standard.EnumParser.enumParser
import org.incendo.cloud.parser.standard.StringParser.stringParser
import org.incendo.cloud.suggestion.SuggestionProvider.blockingStrings

private val MODEL_SUGGESTION = blockingStrings<Audience> { _, _ -> BetterModel.modelKeys() }
private val LIMB_SUGGESTION = blockingStrings<Audience> { _, _ -> BetterModel.limbKeys() }

fun startBukkitCommand() {
    command(
        LegacyPaperCommandManager(
            PLUGIN,
            ExecutionCoordinator.simpleCoordinator(),
            SenderMapper.create<CommandSender, Audience>(
                { sender -> if (sender is Player) AudiencePlayer(sender) else AudienceSender(sender) },
                { audience -> (audience as BukkitAudience).sender }
            )
        ).apply {
            if (hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
                registerBrigadier()
                brigadierManager().setNativeNumberSuggestions(true)
            } else if (hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) registerAsynchronousCompletions()
        },
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
                .optional("type", enumParser(EntityType::class.java))
                .optional("scale", doubleParser(0.0625, 16.0))
                .optional("location", locationParser())
                .senderType(AudiencePlayer::class.java)
                .handler(::spawn)
        }
        create(
            "test",
            "Tests some model's animation to specific player",
            "t"
        ) {
            required("model", stringParser(), MODEL_SUGGESTION)
                .required(
                    "animation",
                    stringParser(),
                    blockingStrings { ctx, _ -> ctx.nullableString("model") { BetterModel.modelOrNull(it)?.animations()?.keys } ?: emptySet()  }
                )
                .optional("player", playerParser())
                .optional("location", locationParser())
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
                .optional("model", stringParser(), blockingStrings { ctx, _ -> ctx.sender().sender.toRegistry()?.trackers()?.map(Tracker::name) ?: emptyList() })
                .handler(::undisguise)
        }
        create(
            "play",
            "Plays player animation",
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
        create(
            "hide",
            "Hides some entities from target player."
        ) {
            required("model", stringParser(), MODEL_SUGGESTION)
                .required("player", playerParser())
                .required("entities", multipleEntitySelectorParser())
                .handler(::hide)
        }
        create(
            "show",
            "Shows some entities to target player."
        ) {
            required("model", stringParser(), MODEL_SUGGESTION)
                .required("player", playerParser())
                .required("entities", multipleEntitySelectorParser())
                .handler(::show)
        }
        create(
            "version",
            "Checks BetterModel's version",
            "v"
        ) {
            handler(::version)
        }
    }
}

private fun hide(context: CommandContext<Audience>) {
    val sender = context.sender()
    val model = context.get<String>("model")
    val player = context.get<Player>("player").wrap()
    var success = false
    context.get<MultipleEntitySelector>("entities").values().forEach {
        if (it.toRegistry()?.tracker(model)?.hide(player) == true) success = true
    }
    if (!success) sender.warn("Failed to hide any of provided entities.")
}

private fun show(context: CommandContext<Audience>) {
    val sender = context.sender()
    val model = context.get<String>("model")
    val player = context.get<Player>("player").wrap()
    var success = false
    context.get<MultipleEntitySelector>("entities").values().forEach {
        if (it.toRegistry()?.tracker(model)?.show(player) == true) success = true
    }
    if (!success) sender.warn("Failed to show any of provided entities.")
}

private fun disguise(context: CommandContext<AudiencePlayer>) {
    val audience = context.sender()
    val player = audience.sender
    val scaling = if (context.getOrDefault("scaling", true)) ModelScaler.entity() else ModelScaler.defaultScaler()
    context.model("model") { return audience.warn("Unable to find this model: $it") }.getOrCreate(player.wrap(), TrackerModifier.DEFAULT) {
        it.scaler(scaling)
    }
}

private fun undisguise(context: CommandContext<AudiencePlayer>) {
    val audience = context.sender()
    val player = audience.sender
    val model = context.nullable<String>("model")
    if (model != null) {
        player.toTracker(model)?.close() ?: audience.warn("Cannot find this model to undisguise: $model")
    } else player.toRegistry()?.close() ?: audience.warn("Cannot find any model to undisguise")
}

private fun spawn(context: CommandContext<AudiencePlayer>) {
    val audience = context.sender()
    val player = audience.sender
    val model = context.model("model") { return audience.warn("Unable to find this model: $it") }
    val type = context.nullable("type", EntityType.HUSK)
    val scale = context.nullable("scale", 1.0)
    val loc = context.nullable("location") { player.location }
    loc.run {
        (world ?: player.world).spawnEntity(
            this,
            type
        )
    }.takeIf {
        it.isValid
    }?.let { entity ->
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
    val player = audience.sender
    val limb = context.limb("limb") { return audience.warn("Unable to find this limb: $it") }
    val animation = context.string("animation") { limb.animation(it).orElse(null) ?: return audience.warn("Unable to find this animation: $it") }
    val loopType = context.nullable("loop_type", AnimationIterator.Type.PLAY_ONCE)
    val hide = context.nullable<Boolean>("hide") != false
    limb.getOrCreate(player.wrap(), TrackerModifier.DEFAULT) {
        it.hideOption(if (hide) EntityHideOption.DEFAULT else EntityHideOption.FALSE)
    }.run {
        if (!animate(animation, AnimationModifier(0, 0, loopType), ::close)) close()
    }
}

private fun test(context: CommandContext<Audience>) {
    val audience = context.sender()
    val model = context.model("model") { return audience.warn("Unable to find this model: $it") }
    val animation = context.string("animation") { str -> model.animation(str).orElse(null) ?: return audience.warn("Unable to find this animation: $str") }
    val player = context.nullable("player") { audience as? Player ?: return audience.warn("Unable to find target player.") }
    val location = context.nullable("location") {
        player.location.apply {
            add(Vector(0, 0, 10).rotateAroundY(-Math.toRadians(yaw.toDouble())))
            yaw += 180
        }
    }
    model.create(location.wrap()).run {
        spawn(player.wrap())
        animate(animation, AnimationModifier(0, 0, AnimationIterator.Type.PLAY_ONCE), ::close)
    }
}
