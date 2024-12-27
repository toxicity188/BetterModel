package kr.toxicity.model.manager

import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIBukkitConfig
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.CommandExecutionInfo
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import kr.toxicity.model.api.BetterModel.ReloadResult.*
import kr.toxicity.model.api.manager.CommandManager
import kr.toxicity.model.api.util.EntityUtil
import kr.toxicity.model.util.PLUGIN
import org.bukkit.entity.EntityType

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
                            ?.let {
                                loc.getNearbyPlayers(EntityUtil.RENDER_DISTANCE).forEach { p ->
                                    it.spawn(p)
                                }
                            }
                            ?: run {
                                player.sendMessage("Unable to find this renderer: $n")
                            }
                    }),
                CommandAPICommand("reload")
                    .withAliases("re", "rl")
                    .withPermission("bettermodel.reload")
                    .executes(CommandExecutionInfo {
                        when (val result = PLUGIN.reload()) {
                            is OnReload -> it.sender().sendMessage("The plugin still on reload!")
                            is Success -> it.sender().sendMessage("Reload completed (${result.time} time)")
                            is Failure -> {
                                it.sender().sendMessage("Reload failed.")
                                it.sender().sendMessage("Reason: ${result.throwable.message ?: result.throwable.javaClass.simpleName}")
                            }
                        }
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