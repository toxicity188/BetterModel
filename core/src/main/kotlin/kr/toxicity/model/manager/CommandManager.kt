/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.manager

import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.BetterModelPlugin.ReloadResult.*
import kr.toxicity.model.api.animation.AnimationIterator
import kr.toxicity.model.api.animation.AnimationModifier
import kr.toxicity.model.api.pack.PackZipper
import kr.toxicity.model.api.tracker.EntityHideOption
import kr.toxicity.model.api.tracker.Tracker
import kr.toxicity.model.api.tracker.TrackerModifier
import kr.toxicity.model.api.version.MinecraftVersion
import kr.toxicity.model.command.*
import kr.toxicity.model.util.*
import net.kyori.adventure.text.format.NamedTextColor.*
import org.bukkit.command.CommandSender
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.incendo.cloud.bukkit.data.MultipleEntitySelector
import org.incendo.cloud.bukkit.parser.PlayerParser
import org.incendo.cloud.bukkit.parser.location.LocationParser
import org.incendo.cloud.bukkit.parser.selector.MultipleEntitySelectorParser
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.parser.standard.BooleanParser
import org.incendo.cloud.parser.standard.DoubleParser
import org.incendo.cloud.parser.standard.EnumParser
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.suggestion.SuggestionProvider

object CommandManager : GlobalManager {

    private val modelSuggestion = SuggestionProvider.blockingStrings<CommandSender> { _, _ -> BetterModel.modelKeys() }
    private val limbSuggestion = SuggestionProvider.blockingStrings<CommandSender> { _, _ -> BetterModel.limbKeys() }

    override fun start() {
        command(
            "bettermodel",
            "BetterModel's main command.",
            "bm", "model"
        ) {
            create(
                "reload",
                "Reloads this plugin.",
                "re", "rl"
            ) {
                handler(::reload)
            }
            create(
                "spawn",
                "Summons some model to given type",
                "s"
            ) {
                required("model", StringParser.stringParser(), modelSuggestion)
                    .optional("type", EnumParser.enumParser(EntityType::class.java))
                    .optional("scale", DoubleParser.doubleParser(0.0625, 16.0))
                    .optional("location", LocationParser.locationParser())
                    .senderType(Player::class.java)
                    .handler(::spawn)
            }
            create(
                "test",
                "Tests some model's animation to specific player",
                "t"
            ) {
                required("model", StringParser.stringParser(), modelSuggestion)
                    .required(
                        "animation",
                        StringParser.stringParser(),
                        SuggestionProvider.blockingStrings { ctx, _ -> ctx.nullableString("model") { BetterModel.modelOrNull(it)?.animations()?.keys } ?: emptySet()  }
                    )
                    .optional("player", PlayerParser.playerParser())
                    .optional("location", LocationParser.locationParser())
                    .handler(::test)
            }
            create(
                "disguise",
                "Disguises self.",
                "d"
            ) {
                required("model", StringParser.stringParser(), modelSuggestion)
                    .senderType(Player::class.java)
                    .handler(::disguise)
            }
            create(
                "undisguise",
                "Undisguises self.",
                "ud"
            ) {
                senderType(Player::class.java)
                    .optional("model", StringParser.stringParser(), SuggestionProvider.blockingStrings { ctx, _ -> ctx.sender().toRegistry()?.trackers()?.map(Tracker::name) ?: emptyList() })
                    .handler(::undisguise)
            }
            create(
                "play",
                "Plays player animation",
                "p"
            ) {
                required("limb", StringParser.stringParser(), limbSuggestion)
                    .required(
                        "animation",
                        StringParser.stringParser(),
                        SuggestionProvider.blockingStrings { ctx, _ -> ctx.nullableString("limb") { BetterModel.limbOrNull(it)?.animations()?.keys } ?: emptySet()  }
                    )
                    .optional("loop_type", EnumParser.enumParser(AnimationIterator.Type::class.java))
                    .optional("hide", BooleanParser.booleanParser())
                    .senderType(Player::class.java)
                    .handler(::play)
            }
            create(
                "hide",
                "Hides some entities from target player."
            ) {
                required("model", StringParser.stringParser(), modelSuggestion)
                    .required("player", PlayerParser.playerParser())
                    .required("entities", MultipleEntitySelectorParser.multipleEntitySelectorParser())
                    .handler(::hide)
            }
            create(
                "show",
                "Shows some entities to target player."
            ) {
                required("model", StringParser.stringParser(), modelSuggestion)
                    .required("player", PlayerParser.playerParser())
                    .required("entities", MultipleEntitySelectorParser.multipleEntitySelectorParser())
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

    private fun hide(context: CommandContext<CommandSender>) {
        val sender = context.sender()
        val model = context.get<String>("model")
        val player = context.get<Player>("player")
        var success = false
        context.get<MultipleEntitySelector>("entities").values().forEach {
            if (it.toRegistry()?.tracker(model)?.hide(player) == true) success = true
        }
        if (!success) sender.audience().warn("Failed to hide any of provided entities.")
    }

    private fun show(context: CommandContext<CommandSender>) {
        val sender = context.sender()
        val model = context.get<String>("model")
        val player = context.get<Player>("player")
        var success = false
        context.get<MultipleEntitySelector>("entities").values().forEach {
            if (it.toRegistry()?.tracker(model)?.show(player) == true) success = true
        }
        if (!success) sender.audience().warn("Failed to show any of provided entities.")
    }

    private fun disguise(context: CommandContext<Player>) {
        val player = context.sender()
        context.model("model") { return player.audience().warn("Unable to find this model: $it") }.getOrCreate(player)
    }

    private fun undisguise(context: CommandContext<Player>) {
        val player = context.sender()
        val model = context.nullable<String>("model")
        if (model != null) {
            player.toTracker(model)?.close() ?: player.audience().warn("Cannot find this model to undisguise: $model")
        } else player.toRegistry()?.close() ?: player.audience().warn("Cannot find any model to undisguise")
    }

    private fun spawn(context: CommandContext<Player>) {
        val player = context.sender()
        val model = context.model("model") { return player.audience().warn("Unable to find this model: $it") }
        val type = context.nullable("type", EntityType.HUSK)
        val scale = context.nullable("scale", 1.0)
        val loc = context.nullable("location") { player.location }
        loc.run {
            (world ?: player.world).spawnEntity(
                this,
                type
            ).apply {
                if (PLUGIN.version() >= MinecraftVersion.V1_21 && this is LivingEntity) getAttribute(ATTRIBUTE_SCALE)?.baseValue = scale
            }
        }.takeIf {
            it.isValid
        }?.let { entity ->
            model.create(entity)
        } ?: player.audience().warn("Entity spawning has been blocked.")
    }

    private fun version(context: CommandContext<CommandSender>) {
        val sender = context.sender()
        val audience = sender.audience()
        audience.info("Searching version, please wait...")
        PLUGIN.scheduler().asyncTask {
            val version = LATEST_VERSION
            audience.infoNotNull(
                emptyComponentOf(),
                "Current: ${PLUGIN.semver()}".toComponent(),
                version.release?.let { version -> componentOf("Latest release: ") { append(version.toURLComponent()) } },
                version.snapshot?.let { version -> componentOf("Latest snapshot: ") { append(version.toURLComponent()) } }
            )
        }
    }

    private fun reload(context: CommandContext<CommandSender>) {
        val sender = context.sender()
        val audience = sender.audience()
        PLUGIN.scheduler().asyncTask {
            audience.info("Start reloading. please wait...")
            when (val result = PLUGIN.reload(sender)) {
                is OnReload -> audience.warn("This plugin is still on reload!")
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

    private fun play(context: CommandContext<Player>) {
        val player = context.sender()
        val audience = player.audience()
        val limb = context.limb("limb") { return audience.warn("Unable to find this limb: $it") }
        val animation = context.string("animation") { limb.animation(it).orElse(null) ?: return audience.warn("Unable to find this animation: $it") }
        val loopType = context.nullable("loop_type", AnimationIterator.Type.PLAY_ONCE)
        val hide = context.nullable<Boolean>("hide") != false
        limb.getOrCreate(player, TrackerModifier.DEFAULT) {
            it.hideOption(if (hide) EntityHideOption.DEFAULT else EntityHideOption.FALSE)
        }.run {
            if (!animate(animation, AnimationModifier(0, 0, loopType), ::close)) close()
        }
    }

    private fun test(context: CommandContext<CommandSender>) {
        val sender = context.sender()
        val audience = sender.audience()
        val model = context.model("model") { return audience.warn("Unable to find this model: $it") }
        val animation = context.string("animation") { str -> model.animation(str).orElse(null) ?: return audience.warn("Unable to find this animation: $str") }
        val player = context.nullable("player") { sender as? Player ?: return audience.warn("Unable to find target player.") }
        val location = context.nullable("location") {
            player.location.apply {
                add(Vector(0, 0, 10).rotateAroundY(-Math.toRadians(yaw.toDouble())))
                yaw += 180
            }
        }
        model.create(location).run {
            spawn(player)
            animate(animation, AnimationModifier(0, 0, AnimationIterator.Type.PLAY_ONCE), ::close)
        }
    }

    override fun reload(pipeline: ReloadPipeline, zipper: PackZipper) {

    }

    override fun end() {
    }
}