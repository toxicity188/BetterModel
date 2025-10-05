/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.manager

import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.arguments.*
import dev.jorel.commandapi.executors.CommandExecutionInfo
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.BetterModelPlugin.ReloadResult.*
import kr.toxicity.model.api.animation.AnimationIterator
import kr.toxicity.model.api.animation.AnimationModifier
import kr.toxicity.model.api.manager.CommandManager
import kr.toxicity.model.api.pack.PackZipper
import kr.toxicity.model.api.tracker.EntityHideOption
import kr.toxicity.model.api.version.MinecraftVersion
import kr.toxicity.model.command.commandModule
import kr.toxicity.model.util.*
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor.*
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import java.util.concurrent.CompletableFuture
import kotlin.math.pow

object CommandManagerImpl : CommandManager, GlobalManager {

    override fun start() {
        commandModule("bettermodel") {
            withAliases("bm")
        }.apply {
            command("disguise") {
                withShortDescription("disguises self.")
                withAliases("d")
                withArguments(StringArgument("name")
                    .replaceSuggestions(ArgumentSuggestions.strings {
                        BetterModel.modelKeys().toTypedArray()
                    })
                )
                executesPlayer(PlayerCommandExecutor { player, args ->
                    val name = args["name"] as String
                    BetterModel.modelOrNull(name)
                        ?.getOrCreate(player) ?: player.audience().warn("This model doesn't exist: $name")
                })
            }
            command("undisguise") {
                withShortDescription("undisguises self.")
                withAliases("ud")
                withOptionalArguments(StringArgument("model")
                    .replaceSuggestions(ArgumentSuggestions.strings {
                        (it.sender as? Player)?.uniqueId?.let { u ->
                            BetterModel.registryOrNull(u)?.trackers()?.map { t ->
                                t.name()
                            }
                        }?.toTypedArray() ?: emptyArray()
                    })
                )
                executesPlayer(PlayerCommandExecutor { player, args ->
                    val model = args.get("model") as? String
                    if (model != null) {
                        player.toTracker(model)?.close() ?: player.audience().warn("Cannot find this model to undisguise: $model")
                    } else BetterModel.registryOrNull(player.uniqueId)?.close() ?: player.audience().warn("Cannot find any model to undisguise")
                })
            }
            command("spawn") {
                withShortDescription("summons some model to given type")
                withAliases("s")
                withArguments(StringArgument("name")
                    .replaceSuggestions(ArgumentSuggestions.strings {
                        BetterModel.modelKeys().toTypedArray()
                    })
                )
                withOptionalArguments(StringArgument("type")
                    .replaceSuggestions(ArgumentSuggestions.strings {
                        EntityType.entries
                            .map {
                                it.name.lowercase()
                            }.toTypedArray()
                    })
                )
                withOptionalArguments(DoubleArgument("scale")
                    .replaceSuggestions(ArgumentSuggestions.strings((-2..2).map {
                        4.0.pow(it.toDouble()).toString()
                    }))
                )
                withOptionalArguments(LocationArgument("coordinates"))
                executesPlayer(PlayerCommandExecutor { player, args ->
                    val n = args["name"] as String
                    val t = (args["type"] as? String)?.let {
                        runCatching {
                            EntityType.valueOf((args["type"] as String).uppercase())
                        }.getOrDefault(EntityType.HUSK)
                    } ?: EntityType.HUSK
                    val s = args["scale"] as? Double ?: 1.0
                    val loc = args["coordinates"] as? Location ?: player.location
                    val e = player.world.spawnEntity(loc, t).apply {
                        if (PLUGIN.version() >= MinecraftVersion.V1_21 && this is LivingEntity) getAttribute(ATTRIBUTE_SCALE)?.baseValue = s
                    }
                    if (e.isValid) {
                        BetterModel.modelOrNull(n)
                            ?.create(e)
                            ?: player.audience().warn("Unable to find this renderer: $n")
                    } else {
                        player.audience().warn("Entity spawning has been blocked.")
                    }
                })
            }
            command("reload") {
                withAliases("re", "rl")
                withShortDescription("reloads this plugin.")
                executes(CommandExecutionInfo {
                    PLUGIN.scheduler().asyncTask {
                        val audience = it.sender().audience()
                        audience.info("Start reloading. please wait...")
                        when (val result = PLUGIN.reload(it.sender())) {
                            is OnReload -> audience.warn("This plugin is still on reload!")
                            is Success -> {
                                audience.info(
                                    emptyComponentOf(),
                                    componentOf("Reload completed. (${result.totalTime().withComma()}ms)") {
                                        color(GREEN)
                                    },
                                    componentOf("Assets reload time - ${result.assetsTime().withComma()}ms") {
                                        color(GRAY)
                                        hoverEvent(HoverEvent.showText(componentOf("Reading all config and model.")))
                                    },
                                    componentOf("Packing time - ${result.packingTime().withComma()}ms") {
                                        color(GRAY)
                                        hoverEvent(HoverEvent.showText(componentOf("Packing all model to resource pack.")))
                                    },
                                    componentOf("${BetterModel.models().size.withComma()} of models are loaded successfully. (${result.length().toByteFormat()})") {
                                        color(YELLOW)
                                    },
                                    componentOf(if (result.packResult.changed()) "${result.packResult.size().withComma()} of files are zipped." else "Zipping is skipped due to the same result.") {
                                        color(YELLOW)
                                    },
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
                })
            }
            command("version") {
                withShortDescription("checks BetterModel's version.")
                withAliases("v")
                executes(CommandExecutionInfo {
                    val audience = it.sender().audience()
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
                })
            }
            command("play") {
                withShortDescription("plays player animation.")
                withAliases("p")
                withArguments(
                    StringArgument("name")
                        .replaceSuggestions(ArgumentSuggestions.strings {
                            BetterModel.limbKeys().toTypedArray()
                        }),
                    StringArgument("animation")
                        .replaceSuggestions { sender, builder ->
                            BetterModel.limbOrNull(sender.previousArgs["name"] as String)?.animations()?.forEach(builder::suggest)
                            CompletableFuture.supplyAsync {
                                builder.build()
                            }
                        }
                )
                withOptionalArguments(
                    StringArgument("loop_type")
                        .replaceSuggestions(ArgumentSuggestions.strings(
                            *AnimationIterator.Type.entries.map { it.name.lowercase() }.toTypedArray()
                        )),
                    BooleanArgument("hide")
                )
                executesPlayer(PlayerCommandExecutor { player, args ->
                    val n = args["name"] as String
                    val a = args["animation"] as String
                    val loopTypeStr = args["loop_type"] as? String
                    val hide = args["hide"] as? Boolean != false

                    val loopType = loopTypeStr?.let {
                        runCatching {
                            AnimationIterator.Type.valueOf(it.uppercase())
                        }.onFailure {
                            player.audience().warn("Invalid loop type: '$loopTypeStr'. Using default.")
                        }.getOrNull()
                    }
                    if (!ModelManagerImpl.animate(player, n, a, AnimationModifier(
                        1,
                        0,
                        loopType
                    )) {
                        it.hideOption(if (hide) EntityHideOption.DEFAULT else EntityHideOption.FALSE)
                    }) player.audience().warn("Unable to find this animation($a) or model($n).")
                })
            }
            command("test") {
                withShortDescription("Tests some model's animation to specific player")
                withAliases("t")
                withArguments(
                    StringArgument("model")
                        .replaceSuggestions(ArgumentSuggestions.strings {
                            BetterModel.modelKeys().toTypedArray()
                        }),
                    StringArgument("animation")
                        .replaceSuggestions { sender, builder ->
                            BetterModel.modelOrNull(sender.previousArgs["model"] as String)?.animations()?.forEach(builder::suggest)
                            CompletableFuture.supplyAsync {
                                builder.build()
                            }
                        }
                )
                withOptionalArguments(
                    EntitySelectorArgument.OnePlayer("player"),
                    LocationArgument("location")
                )
                executes(CommandExecutionInfo info@ {
                    val audience = it.sender().audience()
                    val model = (it.args()["model"] as String).run {
                        BetterModel.modelOrNull(this) ?: return@info audience.warn("Unable to find this model: $this")
                    }
                    val animation = (it.args()["animation"] as String).run {
                        model.animation(this).orElse(null) ?: return@info audience.warn("Unable to find this animation: $this")
                    }
                    val player = it.args()["player"] as? Player ?: it.sender() as? Player ?: return@info audience.warn("Unable to find target player.")
                    val location = it.args()["location"] as? Location ?: player.location.apply {
                        add(Vector(0, 0, 10).rotateAroundY(-Math.toRadians(player.yaw.toDouble())))
                        yaw += 180
                    }
                    model.create(location).run {
                        spawn(player)
                        animate({ true }, animation, AnimationModifier.builder()
                            .start(0)
                            .type(AnimationIterator.Type.PLAY_ONCE)
                            .build(), ::close)
                    }
                })
            }
        }.build().register(PLUGIN)
        CommandAPI.onEnable()
    }

    override fun reload(pipeline: ReloadPipeline, zipper: PackZipper) {

    }

    override fun end() {
        CommandAPI.onDisable()
    }
}