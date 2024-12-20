package kr.toxicity.model.manager

import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIBukkitConfig
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import kr.toxicity.model.api.manager.CommandManager
import kr.toxicity.model.util.PLUGIN
import org.bukkit.entity.EntityType

object CommandManagerImpl : CommandManager, GlobalManagerImpl {
    override fun start() {
        CommandAPI.onLoad(CommandAPIBukkitConfig(PLUGIN).silentLogs(true))
        CommandAPICommand("modelrenderer")
            .withAliases("mr")
            .withPermission("modelrenderer")
            .withSubcommands(CommandAPICommand("orc")
                .withAliases("o")
                .executesPlayer(PlayerCommandExecutor { player, _ ->
                    val renderer = ModelManagerImpl.renderer("orc_warrior")!!
                    renderer.create(player.world.spawnEntity(player.location, EntityType.HUSK)).spawn(player)
                })
            )
            .executesPlayer(PlayerCommandExecutor { player, _ ->
                player.sendMessage("/modelrenderer orc - summons test orc.")
            })
            .register(PLUGIN)
    }

    override fun reload() {

    }
}