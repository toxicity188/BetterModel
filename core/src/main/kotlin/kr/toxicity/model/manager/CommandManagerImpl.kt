package kr.toxicity.model.manager

import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIBukkitConfig
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.DoubleArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.CommandExecutionInfo
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.BetterModelPlugin.ReloadResult.*
import kr.toxicity.model.api.animation.AnimationIterator
import kr.toxicity.model.api.animation.AnimationModifier
import kr.toxicity.model.api.manager.CommandManager
import kr.toxicity.model.api.manager.ReloadInfo
import kr.toxicity.model.api.pack.PackZipper
import kr.toxicity.model.api.tracker.EntityTrackerRegistry
import kr.toxicity.model.api.version.MinecraftVersion
import kr.toxicity.model.command.commandModule
import kr.toxicity.model.util.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture
import kotlin.math.pow

object CommandManagerImpl : CommandManager, GlobalManagerImpl {

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
                        ModelManagerImpl.keys().toTypedArray()
                    })
                )
                executesPlayer(PlayerCommandExecutor { player, args ->
                    val name = args.get("name") as String
                    ModelManagerImpl.renderer(name)
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
                        ModelManagerImpl.keys().toTypedArray()
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
                    ModelManagerImpl.renderer(n)
                        ?.create(player.world.spawnEntity(player.location, t).apply {
                            if (PLUGIN.version() >= MinecraftVersion.V1_21 && this is LivingEntity) getAttribute(ATTRIBUTE_SCALE)?.baseValue = s
                        })
                        ?: player.audience().warn("Unable to find this renderer: $n")
                })
            }
            command("reload") {
                withAliases("re", "rl")
                withShortDescription("reloads this plugin.")
                executes(CommandExecutionInfo {
                    PLUGIN.scheduler().asyncTask {
                        val audience = it.sender().audience()
                        audience.info("Start reloading. please wait...")
                        when (val result = PLUGIN.reload()) {
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
                                    componentOf("${result.packData.bytes.size.withComma()} of files are zipped.") {
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
            command("play") {
                withShortDescription("plays player animation.")
                withAliases("p")
                withArguments(
                    StringArgument("name")
                        .replaceSuggestions(ArgumentSuggestions.strings {
                            PlayerManagerImpl.limbs().map {
                                it.name()
                            }.toTypedArray()
                        }),
                    StringArgument("animation")
                        .replaceSuggestions { sender, builder ->
                            PlayerManagerImpl.limb(sender.previousArgs["name"] as String)?.animations()?.forEach(builder::suggest)
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
                    if (!PlayerManagerImpl.animate(player, n, a, AnimationModifier(
                        { true },
                        1,
                        0,
                        loopType,
                        1.0F
                    ))) player.audience().warn("Unable to find this animation($a) or model($n).")
                })
            }
        }.build().register(PLUGIN)
        CommandAPI.onEnable()
    }

    override fun reload(info: ReloadInfo, zipper: PackZipper) {

    }

    override fun end() {
        CommandAPI.onDisable()
    }
}