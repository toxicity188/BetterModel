/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.manager

import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.arguments.*
import dev.jorel.commandapi.executors.CommandArguments
import dev.jorel.commandapi.executors.CommandExecutor
import dev.jorel.commandapi.executors.PlayerCommandExecutor
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
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import kotlin.math.pow

object CommandManager : GlobalManager {

    private val modelKeys get() = StringArgument("model").suggest { BetterModel.modelKeys() }
    private val limbKeys get() = StringArgument("limb").suggest { BetterModel.limbKeys() }
    private val playerArgs get() = EntitySelectorArgument.OnePlayer("player")
    private val entitiesArgs get() = EntitySelectorArgument.ManyEntities("entities")

    override fun start() {
        commandModule("bettermodel") {
            withAliases("bm")
        }.apply {
            command("reload") {
                withAliases("re", "rl")
                withShortDescription("reloads this plugin.")
                executes(CommandExecutor { sender, _ -> reload(sender) })
            }
            command("spawn") {
                withShortDescription("summons some model to given type")
                withAliases("s")
                withArguments(modelKeys)
                withOptionalArguments(
                    EntityTypeArgument("type"),
                    DoubleArgument("scale").suggest((-2..2).map { 4.0.pow(it.toDouble()).toString() }),
                    LocationArgument("location")
                )
                executesPlayer(PlayerCommandExecutor(::spawn))
            }
            command("test") {
                withShortDescription("Tests some model's animation to specific player")
                withAliases("t")
                withArguments(
                    modelKeys,
                    StringArgument("animation").suggestNullable {
                        it.previousArgs.mapNullableString("model") { model -> BetterModel.modelOrNull(model) }?.animations()?.keys
                    }
                )
                withOptionalArguments(
                    playerArgs,
                    LocationArgument("location")
                )
                executes(CommandExecutor(::test))
            }
            command("disguise") {
                withShortDescription("disguises self.")
                withAliases("d")
                withArguments(modelKeys)
                executesPlayer(PlayerCommandExecutor(::disguise))
            }
            command("undisguise") {
                withShortDescription("undisguises self.")
                withAliases("ud")
                withOptionalArguments(
                    StringArgument("model").suggestNullable { (it.sender as? Player)?.toRegistry()?.trackers()?.map(Tracker::name) }
                )
                executesPlayer(PlayerCommandExecutor(::undisguise))
            }
            command("play") {
                withShortDescription("plays player animation.")
                withAliases("p")
                withArguments(
                    limbKeys,
                    StringArgument("animation").suggestNullable {
                        it.previousArgs.mapNullableString("limb") { model -> BetterModel.limbOrNull(model) }?.animations()?.keys
                    }
                )
                withOptionalArguments(
                    StringArgument("loop_type").suggest(AnimationIterator.Type.entries.map { it.name.lowercase() }),
                    BooleanArgument("hide")
                )
                executesPlayer(PlayerCommandExecutor(::play))
            }
            command("hide") {
                withShortDescription("hides some entities from target player.")
                withArguments(modelKeys, playerArgs, entitiesArgs)
                executes(CommandExecutor(::hide))
            }
            command("show") {
                withShortDescription("shows some entities to target player.")
                withArguments(modelKeys, playerArgs, entitiesArgs)
                executes(CommandExecutor(::show))
            }
            command("version") {
                withShortDescription("checks BetterModel's version.")
                withAliases("v")
                executes(CommandExecutor { sender, _ -> version(sender) })
            }
        }.build().register(PLUGIN)
        CommandAPI.onEnable()
    }

    private fun disguise(player: Player, args: CommandArguments) {
        args.mapToModel("model") { return player.audience().warn("Unable to find this model: $it") }.getOrCreate(player)
    }

    private fun hide(sender: CommandSender, args: CommandArguments) {
        val model = args.map<String>("model")
        val player = args.map<Player>("player")
        if (!args.any<Entity>("entities") {
                it.toRegistry()?.tracker(model)?.hide(player) == true
            }) {
            sender.audience().warn("Failed to hide any of provided entities.")
        }
    }

    private fun show(sender: CommandSender, args: CommandArguments) {
        val model = args.map<String>("model")
        val player = args.map<Player>("player")
        if (!args.any<Entity>("entities") {
                it.toRegistry()?.tracker(model)?.show(player) == true
            }) {
            sender.audience().warn("Failed to hide any of provided entities.")
        }
    }

    private fun undisguise(player: Player, args: CommandArguments) {
        val model = args.mapNullable<String>("model")
        if (model != null) {
            player.toTracker(model)?.close() ?: player.audience().warn("Cannot find this model to undisguise: $model")
        } else player.toRegistry()?.close() ?: player.audience().warn("Cannot find any model to undisguise")
    }

    private fun spawn(player: Player, args: CommandArguments) {
        val model = args.mapToModel("model") { return player.audience().warn("Unable to find this model: $it") }
        val type = args.map("type", EntityType.HUSK)
        val scale = args.map("scale", 1.0)
        val loc = args.map("location") { player.location }
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

    private fun version(sender: CommandSender) {
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

    private fun reload(sender: CommandSender) {
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

    private fun play(player: Player, args: CommandArguments) {
        val audience = player.audience()
        val limb = args.mapToLimb("limb") { return audience.warn("Unable to find this limb: $it") }
        val animation = args.mapString("animation") { limb.animation(it).orElse(null) ?: return audience.warn("Unable to find this animation: $it") }
        val loopType = args.mapNullableString("loop_type") {
            runCatching {
                AnimationIterator.Type.valueOf(it.uppercase())
            }.onFailure { _ ->
                audience.warn("Invalid loop type: '$it'. Using default.")
            }.getOrNull()
        }
        val hide = args.mapNullable<Boolean>("hide") != false
        limb.getOrCreate(player, TrackerModifier.DEFAULT) {
            it.hideOption(if (hide) EntityHideOption.DEFAULT else EntityHideOption.FALSE)
        }.run {
            if (!animate(animation, AnimationModifier(0, 0, loopType), ::close)) close()
        }
    }

    private fun test(sender: CommandSender, args: CommandArguments) {
        val audience = sender.audience()
        val model = args.mapToModel("model") { return audience.warn("Unable to find this model: $it") }
        val animation = args.mapString("animation") { str -> model.animation(str).orElse(null) ?: return audience.warn("Unable to find this animation: $str") }
        val player = args.map("player") { sender as? Player ?: return audience.warn("Unable to find target player.") }
        val location = args.map("location") {
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
        CommandAPI.onDisable()
    }
}