package kr.toxicity.model.manager

import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIBukkitConfig
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.BooleanArgument
import dev.jorel.commandapi.arguments.DoubleArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.CommandExecutionInfo
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import kr.toxicity.model.api.BetterModelPlugin.ReloadResult.*
import kr.toxicity.model.api.manager.CommandManager
import kr.toxicity.model.api.nms.NMSVersion
import kr.toxicity.model.api.tracker.EntityTracker
import kr.toxicity.model.util.ATTRIBUTE_SCALE
import kr.toxicity.model.util.PLUGIN
import kr.toxicity.model.util.handleException
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture
import kotlin.math.pow

object CommandManagerImpl : CommandManager, GlobalManagerImpl {
    override fun start() {
        CommandAPI.onLoad(CommandAPIBukkitConfig(PLUGIN).silentLogs(true))
        CommandAPICommand("bettermodel")
            .withAliases("bm")
            .withPermission("bettermodel")
            .withSubcommands(
                CommandAPICommand("disguise")
                    .withAliases("d")
                    .withPermission("bettermodel.disguise")
                    .withArguments(StringArgument("name")
                        .replaceSuggestions(ArgumentSuggestions.strings {
                            ModelManagerImpl.keys().toTypedArray()
                        })
                    )
                    .executesPlayer(PlayerCommandExecutor { player, args ->
                        EntityTracker.tracker(player)?.close()
                        val name = args.get("name") as String
                        ModelManagerImpl.renderer(name)
                            ?.create(player)
                            ?.spawnNearby(player.location) ?: player.sendMessage("This model doesn't exist: $name")
                    }),
                CommandAPICommand("undisguise")
                    .withAliases("ud")
                    .withPermission("bettermodel.undisguise")
                    .executesPlayer(PlayerCommandExecutor { player, args ->
                        EntityTracker.tracker(player)?.close() ?: player.sendMessage("Cannot find any model to undisguise")
                    }),
                CommandAPICommand("spawn")
                    .withAliases("s")
                    .withPermission("bettermodel.spawn")
                    .withArguments(StringArgument("name")
                        .replaceSuggestions(ArgumentSuggestions.strings {
                            ModelManagerImpl.keys().toTypedArray()
                        })
                    )
                    .withOptionalArguments(StringArgument("type")
                        .replaceSuggestions(ArgumentSuggestions.strings {
                            EntityType.entries
                                .filter {
                                    it.isAlive
                                }
                                .map {
                                    it.name.lowercase()
                                }.toTypedArray()
                        })
                    )
                    .withOptionalArguments(DoubleArgument("scale")
                        .replaceSuggestions(ArgumentSuggestions.strings((-2..2).map {
                            4.0.pow(it.toDouble()).toString()
                        }))
                    )
                    .executesPlayer(PlayerCommandExecutor { player, args ->
                        val n = args["name"] as String
                        val t = (args["type"] as? String)?.let {
                            runCatching {
                                EntityType.valueOf((args["type"] as String).uppercase())
                            }.getOrDefault(EntityType.HUSK)
                        } ?: EntityType.HUSK
                        val s = args["scale"] as? Double ?: 1.0
                        val loc = player.location
                        ModelManagerImpl.renderer(n)
                            ?.create((player.world.spawnEntity(player.location, t) as LivingEntity).apply {
                                if (PLUGIN.nms().version() >= NMSVersion.V1_21_R1) getAttribute(ATTRIBUTE_SCALE)?.baseValue = s
                            })
                            ?.spawnNearby(loc)
                            ?: run {
                                player.sendMessage("Unable to find this renderer: $n")
                            }
                    }),
                CommandAPICommand("reload")
                    .withAliases("re", "rl")
                    .withPermission("bettermodel.reload")
                    .executes(CommandExecutionInfo {
                        PLUGIN.scheduler().asyncTask {
                            when (val result = PLUGIN.reload()) {
                                is OnReload -> it.sender().sendMessage("The plugin still on reload!")
                                is Success -> it.sender().sendMessage("Reload completed (${result.time} ms)")
                                is Failure -> {
                                    it.sender().sendMessage("Reload failed.")
                                    it.sender().sendMessage("Please read the log to find the problem.")
                                    result.throwable.handleException("Reload failed.")
                                }
                            }
                        }
                    }),
                CommandAPICommand("limb")
                    .withAliases("l")
                    .withPermission("bettermodel.limb")
                    .withArguments(BooleanArgument("toggle")
                        .replaceSuggestions { sender, builder ->
                            (sender.sender as? Player)?.let {
                                builder.suggest((!PlayerManagerImpl.player(it).showPlayerLimb()).toString())
                            }
                            CompletableFuture.supplyAsync {
                                builder.build()
                            }
                        })
                    .executesPlayer(PlayerCommandExecutor { player, args ->
                        val t = args["toggle"] as Boolean
                        PlayerManagerImpl.player(player).showPlayerLimb(t)
                        player.sendMessage("Sets player limb to ${if (t) "enable" else "disable"}.")
                    }),
                CommandAPICommand("play")
                    .withAliases("p", "p")
                    .withPermission("bettermodel.play")
                    .withArguments(
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
                    .executesPlayer(PlayerCommandExecutor { player, args ->
                        val n = args["name"] as String
                        val a = args["animation"] as String
                        PlayerManagerImpl.animate(player, n, a)
                    }),
            )
            .executes(CommandExecutionInfo {
                it.sender().sendMessage("/bettermodel reload - reloads this plugin.")
                it.sender().sendMessage("/bettermodel summon <model> [type] [scale] - summons some model to given type.")
                it.sender().sendMessage("/bettermodel disguise <model> - disguises self.")
                it.sender().sendMessage("/bettermodel undisguise <model> - undisguises self.")
                it.sender().sendMessage("/bettermodel limb <true/false> - toggles whether sender can see some player's animation.")
                it.sender().sendMessage("/bettermodel play <model> <animation> - plays player animation.")
            })
            .register(PLUGIN)
        CommandAPI.onEnable()
    }

    override fun reload() {

    }

    override fun end() {
        CommandAPI.onDisable()
    }
}