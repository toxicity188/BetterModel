package kr.toxicity.model.manager

import kr.toxicity.model.api.data.blueprint.BlueprintChildren.BlueprintGroup
import kr.toxicity.model.api.data.blueprint.ModelBlueprint
import kr.toxicity.model.api.animation.AnimationModifier
import kr.toxicity.model.api.data.renderer.ModelRenderer
import kr.toxicity.model.api.data.renderer.RendererGroup
import kr.toxicity.model.api.manager.PlayerManager
import kr.toxicity.model.api.manager.ReloadInfo
import kr.toxicity.model.api.nms.PlayerChannelHandler
import kr.toxicity.model.api.tracker.EntityTracker
import kr.toxicity.model.util.*
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object PlayerManagerImpl : PlayerManager, GlobalManagerImpl {

    private val playerMap = ConcurrentHashMap<UUID, PlayerChannelHandler>()
    private val renderMap = HashMap<String, ModelRenderer>()

    override fun start() {
        registerListener(object : Listener {
            @EventHandler
            fun PlayerJoinEvent.join() {
                if (player.isOnline) runCatching { //For fake player
                    player.register()
                }.handleFailure {
                    "Unable to load ${player.name}'s data."
                }
            }
            @EventHandler
            fun PlayerChangedWorldEvent.change() {
                if (player.isOnline) runCatching {
                    player.register().unregisterAll()
                }.handleFailure {
                    "Unable to refresh ${player.name}'s data."
                }
            }
            @EventHandler
            fun PlayerQuitEvent.quit() {
                playerMap.remove(player.uniqueId)?.close()
            }
        })
    }

    private fun Player.register() = playerMap.computeIfAbsent(uniqueId) {
        PLUGIN.nms().inject(this)
    }

    override fun reload(info: ReloadInfo) {
        renderMap.clear()
        if (ConfigManagerImpl.module().playerAnimation()) {
            val folder = File(DATA_FOLDER, "players")
            if (!folder.exists()) {
                folder.mkdirs()
                PLUGIN.getResource("steve.bbmodel")?.buffered()?.use { input ->
                    File(folder, "steve.bbmodel").outputStream().buffered().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            folder.forEachAllFolder {
                if (it.extension == "bbmodel") {
                    val load = it.toModel()
                    renderMap[load.name] = load.toRenderer()
                }
            }
        }
    }

    override fun limbs(): List<ModelRenderer> = renderMap.values.toList()
    override fun limb(name: String): ModelRenderer? = renderMap[name]

    override fun animate(player: Player, model: String, animation: String) {
        renderMap[model]?.let {
            EntityTracker.tracker(player.uniqueId)?.close()
            val create = it.create(player)
            create.spawnNearby()
            if (!create.animate(animation, AnimationModifier.DEFAULT) {
                create.close()
            }) create.close()
        }
    }

    private fun ModelBlueprint.toRenderer(): ModelRenderer {
        fun BlueprintGroup.parse(): RendererGroup {
            return RendererGroup(
                boneName(),
                scale,
                ItemStack(Material.AIR),
                this,
                children.mapNotNull {
                    if (it is BlueprintGroup) {
                        it.boneName() to it.parse()
                    } else null
                }.toMap(),
                hitBox()
            )
        }
        return ModelRenderer(this, group.mapNotNull {
            if (it is BlueprintGroup) {
                it.boneName() to it.parse()
            } else null
        }.toMap(), animations)
    }

    override fun player(uuid: UUID): PlayerChannelHandler? = playerMap[uuid]
    override fun player(player: Player): PlayerChannelHandler = player.register()
}