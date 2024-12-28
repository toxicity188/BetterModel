package kr.toxicity.model.manager

import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIBukkitConfig
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.BooleanArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.CommandExecutionInfo
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.BetterModel.ReloadResult.*
import kr.toxicity.model.api.manager.CommandManager
import kr.toxicity.model.api.util.EntityUtil
import kr.toxicity.model.util.PLUGIN
import org.bukkit.Bukkit
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

object CommandManagerImpl : CommandManager, GlobalManagerImpl {
    override fun start() {
        CommandAPI.onLoad(CommandAPIBukkitConfig(PLUGIN).silentLogs(true))
        CommandAPICommand("bettermodel")
            .withAliases("bm")
            .withPermission("bettermodel")
            .withSubcommands(
                CommandAPICommand("spawn")
                    .withAliases("s")
                    .withPermission("bettermodel.spawn")
                    .withArguments(StringArgument("name")
                        .replaceSuggestions(ArgumentSuggestions.strings {
                            ModelManagerImpl.keys().toTypedArray()
                        })
                    )
                    .executesPlayer(PlayerCommandExecutor { player, args ->
                        val n = args["name"] as String
                        val loc = player.location
                        ModelManagerImpl.renderer(n)
                            ?.create(player.world.spawnEntity(player.location, EntityType.HUSK))
                            ?.spawnNearby(loc)
                            ?: run {
                                player.sendMessage("Unable to find this renderer: $n")
                            }
                    }),
                CommandAPICommand("reload")
                    .withAliases("re", "rl")
                    .withPermission("bettermodel.reload")
                    .executes(CommandExecutionInfo {
                        Bukkit.getAsyncScheduler().runNow(BetterModel.inst()) { _ ->
                            when (val result = PLUGIN.reload()) {
                                is OnReload -> it.sender().sendMessage("The plugin still on reload!")
                                is Success -> it.sender().sendMessage("Reload completed (${result.time} time)")
                                is Failure -> {
                                    it.sender().sendMessage("Reload failed.")
                                    it.sender().sendMessage("Reason: ${result.throwable.message ?: result.throwable.javaClass.simpleName}")
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
                it.sender().sendMessage("/bettermodel summon <model> - summons some model to husk.")
            })
            .register(PLUGIN)
    }

    override fun reload() {

    }
}