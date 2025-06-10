package kr.toxicity.model.manager

import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIBukkitConfig
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.BooleanArgument
import dev.jorel.commandapi.arguments.DoubleArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.CommandExecutionInfo
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.BetterModelPlugin.ReloadResult.*
import kr.toxicity.model.api.animation.AnimationIterator
import kr.toxicity.model.api.manager.CommandManager
import kr.toxicity.model.api.manager.PlayerManager
import kr.toxicity.model.api.manager.ReloadInfo
import kr.toxicity.model.api.tracker.EntityTracker
import kr.toxicity.model.api.version.MinecraftVersion
import kr.toxicity.model.command.commandModule
import kr.toxicity.model.util.ATTRIBUTE_SCALE
import kr.toxicity.model.util.PLUGIN
import kr.toxicity.model.util.audience
import kr.toxicity.model.util.handleException
import kr.toxicity.model.util.info
import kr.toxicity.model.util.warn
import kr.toxicity.model.util.withComma
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
                        ?.create(player)
                        ?.spawnNearby() ?: player.audience().warn("This model doesn't exist: $name")
                })
            }
            command("undisguise") {
                withShortDescription("undisguises self.")
                withAliases("ud")
                executesPlayer(PlayerCommandExecutor { player, args ->
                    EntityTracker.tracker(player)?.close() ?: player.audience().warn("Cannot find any model to undisguise")
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
                        ?.spawnNearby()
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
                            is OnReload -> audience.warn("The plugin still on reload!")
                            is Success -> {
                                audience.info("Reload completed. (${result.time.withComma()} ms)")
                                audience.info("${BetterModel.models().size.withComma()} of models are loaded successfully.")
                            }
                            is Failure -> {
                                audience.warn("Reload failed.")
                                audience.warn("Please read the log to find the problem.")
                                result.throwable.handleException("Reload failed.")
                            }
                        }
                    }
                })
            }
            command("limb") {
                withShortDescription("toggles whether sender can see some player's animation.")
                withAliases("l")
                withArguments(BooleanArgument("toggle")
                    .replaceSuggestions { sender, builder ->
                        (sender.sender as? Player)?.let {
                            builder.suggest((!PlayerManagerImpl.player(it).showPlayerLimb()).toString())
                        }
                        CompletableFuture.supplyAsync {
                            builder.build()
                        }
                    })
                executesPlayer(PlayerCommandExecutor { player, args ->
                    val t = args["toggle"] as Boolean
                    PlayerManagerImpl.player(player).showPlayerLimb(t)
                    player.audience().info("Sets player limb to ${if (t) "enable" else "disable"}.")
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
                    (PlayerManagerImpl as PlayerManager).animate(player, n, a, loopType)
                })
            }
        }.build().register(PLUGIN)
        CommandAPI.onEnable()
    }

    override fun reload(info: ReloadInfo) {

    }

    override fun end() {
        CommandAPI.onDisable()
    }
}