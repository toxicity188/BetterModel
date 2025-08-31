package kr.toxicity.model.manager

import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIBukkitConfig
import dev.jorel.commandapi.arguments.*
import dev.jorel.commandapi.executors.CommandExecutionInfo
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.BetterModelPlugin.ReloadResult.*
import kr.toxicity.model.api.animation.AnimationIterator
import kr.toxicity.model.api.animation.AnimationModifier
import kr.toxicity.model.api.manager.CommandManager
import kr.toxicity.model.api.pack.PackZipper
import kr.toxicity.model.api.tracker.EntityTrackerRegistry
import kr.toxicity.model.api.version.MinecraftVersion
import kr.toxicity.model.command.commandModule
import kr.toxicity.model.util.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import java.util.concurrent.CompletableFuture
import kotlin.math.pow

object CommandManagerImpl : CommandManager, GlobalManager {

    override fun start() {
        CommandAPI.onLoad(CommandAPIBukkitConfig(PLUGIN).silentLogs(true))
        commandModule("bettermodel") {
            withAliases("bm")
        }.apply {
            command("disguise") {
                withShortDescription("disguises self.")
                withAliases("d")
                withArguments(StringArgument("name")
                    .replaceSuggestions(ArgumentSuggestions.strings {
                        ModelManagerImpl.modelKeys().toTypedArray()
                    })
                )
                executesPlayer(PlayerCommandExecutor { player, args ->
                    val name = args.get("name") as String
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
                            EntityTrackerRegistry.registry(u)?.trackers()?.map { t ->
                                t.name()
                            }
                        }?.toTypedArray() ?: emptyArray()
                    })
                )
                executesPlayer(PlayerCommandExecutor { player, args ->
                    val model = args.get("model") as? String
                    if (model != null) {
                        player.toTracker(model)?.close() ?: player.audience().warn("Cannot find this model to undisguise: $model")
                    } else EntityTrackerRegistry.registry(player.uniqueId)?.close() ?: player.audience().warn("Cannot find any model to undisguise")
                })
            }
            command("spawn") {
                withShortDescription("summons some model to given type")
                withAliases("s")
                withArguments(StringArgument("name")
                    .replaceSuggestions(ArgumentSuggestions.strings {
                        ModelManagerImpl.modelKeys().toTypedArray()
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
                executesPlayer(PlayerCommandExecutor { player, args ->
                    val n = args["name"] as String
                    val t = (args["type"] as? String)?.let {
                        runCatching {
                            EntityType.valueOf((args["type"] as String).uppercase())
                        }.getOrDefault(EntityType.HUSK)
                    } ?: EntityType.HUSK
                    val s = args["scale"] as? Double ?: 1.0
                    val e = player.world.spawnEntity(player.location, t).apply {
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
                                        color(NamedTextColor.GREEN)
                                    },
                                    componentOf("Assets reload time - ${result.assetsTime().withComma()}ms") {
                                        color(NamedTextColor.GRAY)
                                        hoverEvent(HoverEvent.showText(Component.text("Reading all config and model.")))
                                    },
                                    componentOf("Packing time - ${result.packingTime().withComma()}ms") {
                                        color(NamedTextColor.GRAY)
                                        hoverEvent(HoverEvent.showText(Component.text("Packing all model to resource pack.")))
                                    },
                                    componentOf("${BetterModel.models().size.withComma()} of models are loaded successfully.") {
                                        color(NamedTextColor.YELLOW)
                                    },
                                    componentOf("${result.packResult.size().withComma()} of files are zipped.") {
                                        color(NamedTextColor.YELLOW)
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
                            ModelManagerImpl.limbKeys().toTypedArray()
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
                        ))
                )
                executesPlayer(PlayerCommandExecutor { player, args ->
                    val n = args["name"] as String
                    val a = args["animation"] as String
                    val loopTypeStr = args.get("loop_type") as? String

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
                    ))) player.audience().warn("Unable to find this animation($a) or model($n).")
                })
            }
            command("test") {
                withShortDescription("Tests some model's animation to specific player")
                withAliases("t")
                withArguments(
                    StringArgument("model")
                        .replaceSuggestions(ArgumentSuggestions.strings {
                            ModelManagerImpl.modelKeys().toTypedArray()
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
                    PlayerArgument("player"),
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
                        animate({ true }, animation, AnimationModifier.DEFAULT_WITH_PLAY_ONCE, ::close)
                        spawn(player)
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