package kr.toxicity.model

import kr.toxicity.model.api.ModelRenderer
import kr.toxicity.model.api.manager.ModelManager
import kr.toxicity.model.api.nms.NMS
import kr.toxicity.model.api.version.MinecraftVersion
import kr.toxicity.model.api.version.MinecraftVersion.*
import kr.toxicity.model.manager.ModelManagerImpl
import kr.toxicity.model.util.warn
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

@Suppress("UNUSED")
class ModelRendererImpl : ModelRenderer() {
    private val version = MinecraftVersion(Bukkit.getBukkitVersion().substringBefore('-'))
    private lateinit var nms: NMS

    override fun onEnable() {
        nms = when (version) {
            V1_21_4 -> kr.toxicity.model.nms.v1_21_R3.NMSImpl()
            V1_21_2, V1_21_3 -> kr.toxicity.model.nms.v1_21_R2.NMSImpl()
            else -> {
                warn(
                    "Unsupported version: $version",
                    "Plugin will be automatically disabled."
                )
                Bukkit.getPluginManager().disablePlugin(this)
                return
            }
        }
        ModelManagerImpl.reload()
        Bukkit.getPluginManager().registerEvents(object : Listener {
            @EventHandler
            fun join(e: PlayerJoinEvent) {
                val renderer = ModelManagerImpl.renderer("mosquito")!!
                renderer.create(e.player.location).instance().spawn(e.player)
            }
        }, this)
    }

    override fun modelManager(): ModelManager = ModelManagerImpl

    override fun version(): MinecraftVersion = version
    override fun nms(): NMS = nms
}