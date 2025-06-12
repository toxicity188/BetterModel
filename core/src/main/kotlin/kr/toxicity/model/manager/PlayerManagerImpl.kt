package kr.toxicity.model.manager

import kr.toxicity.model.api.animation.AnimationModifier
import kr.toxicity.model.api.data.blueprint.BlueprintChildren.BlueprintGroup
import kr.toxicity.model.api.data.blueprint.ModelBlueprint
import kr.toxicity.model.api.data.renderer.ModelRenderer
import kr.toxicity.model.api.data.renderer.RendererGroup
import kr.toxicity.model.api.manager.PlayerManager
import kr.toxicity.model.api.manager.ReloadInfo
import kr.toxicity.model.api.nms.PlayerChannelHandler
import kr.toxicity.model.util.*
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object PlayerManagerImpl : PlayerManager, GlobalManagerImpl {

    private val playerMap = ConcurrentHashMap<UUID, PlayerChannelHandler>()
    private val renderMap = hashMapOf<String, ModelRenderer>()

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
                playerMap.remove(player.uniqueId)?.use {
                    SkinManagerImpl.removeCache(it.profile())
                }
            }
        })
    }

    private fun Player.register() = playerMap.computeIfAbsent(uniqueId) {
        PLUGIN.nms().inject(this)
    }.apply {
        if (SkinManagerImpl.supported()) SkinManagerImpl.getOrRequest(profile())
    }

    override fun reload(info: ReloadInfo) {
        renderMap.clear()
        if (ConfigManagerImpl.module().playerAnimation()) {
            DATA_FOLDER.getOrCreateDirectory("players") { folder ->
                folder.addResourceAs("steve.bbmodel")
            }.forEachAllFolder {
                if (it.extension == "bbmodel") {
                    val load = it.toModel()
                    renderMap[load.name] = load.toRenderer()
                }
            }
        }
    }

    override fun limbs(): List<ModelRenderer> = renderMap.values.toList()
    override fun limb(name: String): ModelRenderer? = renderMap[name]

    override fun animate(player: Player, model: String, animation: String, modifier: AnimationModifier) {
        renderMap[model]?.let {
            val create = it.getOrCreate(player)
            if (!create.animate(animation, modifier) {
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