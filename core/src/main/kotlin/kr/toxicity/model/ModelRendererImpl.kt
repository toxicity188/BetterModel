package kr.toxicity.model

import kr.toxicity.model.api.ModelRenderer
import kr.toxicity.model.api.ModelRenderer.ReloadResult.Failure
import kr.toxicity.model.api.ModelRenderer.ReloadResult.OnReload
import kr.toxicity.model.api.ModelRenderer.ReloadResult.Success
import kr.toxicity.model.api.manager.*
import kr.toxicity.model.api.nms.NMS
import kr.toxicity.model.api.version.MinecraftVersion
import kr.toxicity.model.api.version.MinecraftVersion.*
import kr.toxicity.model.manager.*
import kr.toxicity.model.util.warn
import org.bukkit.Bukkit
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("UNUSED")
class ModelRendererImpl : ModelRenderer() {
    private val version = MinecraftVersion(Bukkit.getBukkitVersion().substringBefore('-'))
    private lateinit var nms: NMS

    private val onReload = AtomicBoolean()

    private val managers by lazy {
        listOf(
            CompatibilityManagerImpl,
            ModelManagerImpl,
            PlayerManagerImpl,
            EntityManagerImpl,
            CommandManagerImpl
        )
    }

    override fun onEnable() {
        nms = when (version) {
            V1_21_4 -> kr.toxicity.model.nms.v1_21_R3.NMSImpl()
            V1_21_2, V1_21_3 -> kr.toxicity.model.nms.v1_21_R2.NMSImpl()
            V1_21, V1_21_1 -> kr.toxicity.model.nms.v1_21_R1.NMSImpl()
            else -> {
                warn(
                    "Unsupported version: $version",
                    "Plugin will be automatically disabled."
                )
                Bukkit.getPluginManager().disablePlugin(this)
                return
            }
        }
        managers.forEach(GlobalManagerImpl::start)
        Bukkit.getAsyncScheduler().runNow(this) {
            reload()
        }
    }

    override fun onDisable() {
        managers.forEach(GlobalManagerImpl::end)
    }

    override fun reload(): ReloadResult {
        if (onReload.get()) return OnReload()
        onReload.set(true)
        val result = runCatching {
            val time = System.currentTimeMillis()
            managers.forEach(GlobalManagerImpl::reload)
            Success(System.currentTimeMillis() - time)
        }.getOrElse {
            Failure(it)
        }
        onReload.set(false)
        return result
    }

    override fun modelManager(): ModelManager = ModelManagerImpl
    override fun playerManager(): PlayerManager = PlayerManagerImpl
    override fun entityManager(): EntityManager = EntityManagerImpl
    override fun commandManager(): CommandManager = CommandManagerImpl
    override fun compatibilityManager(): CompatibilityManager = CompatibilityManagerImpl

    override fun version(): MinecraftVersion = version
    override fun nms(): NMS = nms
}