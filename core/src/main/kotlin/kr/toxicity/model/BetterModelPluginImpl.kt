package kr.toxicity.model

import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.BetterModelPlugin.ReloadResult.*
import kr.toxicity.model.api.BetterModelPlugin
import kr.toxicity.model.api.BetterModelPlugin.ReloadResult
import kr.toxicity.model.api.manager.*
import kr.toxicity.model.api.nms.NMS
import kr.toxicity.model.api.scheduler.ModelScheduler
import kr.toxicity.model.api.version.MinecraftVersion
import kr.toxicity.model.api.version.MinecraftVersion.*
import kr.toxicity.model.manager.*
import kr.toxicity.model.scheduler.PaperScheduler
import kr.toxicity.model.scheduler.StandardScheduler
import kr.toxicity.model.util.forEachAsync
import kr.toxicity.model.util.warn
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer
import java.util.jar.JarFile

@Suppress("UNUSED")
class BetterModelPluginImpl : JavaPlugin(), BetterModelPlugin {
    private val version = MinecraftVersion(Bukkit.getBukkitVersion().substringBefore('-'))
    private lateinit var nms: NMS

    private val onReload = AtomicBoolean()

    private val managers by lazy {
        listOf(
            CompatibilityManagerImpl,
            ConfigManagerImpl,
            ModelManagerImpl,
            PlayerManagerImpl,
            EntityManagerImpl,
            CommandManagerImpl
        )
    }

    private val scheduler = if (BetterModel.IS_PAPER) PaperScheduler() else StandardScheduler()

    private val reloadStartTask = arrayListOf<() -> Unit>()
    private val reloadEndTask = arrayListOf<(ReloadResult) -> Unit>()

    override fun onLoad() {
        BetterModel.inst(this)
    }

    override fun onEnable() {
        nms = when (version) {
            V1_21_4 -> kr.toxicity.model.nms.v1_21_R3.NMSImpl()
            V1_21_2, V1_21_3 -> kr.toxicity.model.nms.v1_21_R2.NMSImpl()
            V1_21, V1_21_1 -> kr.toxicity.model.nms.v1_21_R1.NMSImpl()
            V1_20_5, V1_20_6 -> kr.toxicity.model.nms.v1_20_R4.NMSImpl()
            V1_20_3, V1_20_4 -> kr.toxicity.model.nms.v1_20_R3.NMSImpl()
            V1_20_2 -> kr.toxicity.model.nms.v1_20_R2.NMSImpl()
            V1_20, V1_20_1 -> kr.toxicity.model.nms.v1_20_R1.NMSImpl()
            V1_19_4 -> kr.toxicity.model.nms.v1_19_R3.NMSImpl()
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
        reload()
    }

    override fun onDisable() {
        managers.forEach(GlobalManagerImpl::end)
    }

    override fun reload(): ReloadResult {
        if (onReload.get()) return OnReload()
        onReload.set(true)
        reloadStartTask.forEach {
            it()
        }
        val result = runCatching {
            val time = System.currentTimeMillis()
            managers.forEach(GlobalManagerImpl::reload)
            Success(System.currentTimeMillis() - time)
        }.getOrElse {
            Failure(it)
        }
        onReload.set(false)
        reloadEndTask.forEach {
            it(result)
        }
        return result
    }

    fun loadAssets(prefix: String, consumer: (String, InputStream) -> Unit) {
        JarFile(file).use {
            it.entries().toList().forEachAsync { entry ->
                if (!entry.name.startsWith(prefix)) return@forEachAsync
                if (entry.name.length <= prefix.length + 1) return@forEachAsync
                val name = entry.name.substring(prefix.length + 1)
                if (!entry.isDirectory) it.getInputStream(entry).buffered().use { stream ->
                    consumer(name, stream)
                }
            }
        }
    }

    override fun scheduler(): ModelScheduler = scheduler
    override fun modelManager(): ModelManager = ModelManagerImpl
    override fun playerManager(): PlayerManager = PlayerManagerImpl
    override fun entityManager(): EntityManager = EntityManagerImpl
    override fun commandManager(): CommandManager = CommandManagerImpl
    override fun compatibilityManager(): CompatibilityManager = CompatibilityManagerImpl
    override fun configManager(): ConfigManager = ConfigManagerImpl

    override fun version(): MinecraftVersion = version
    override fun nms(): NMS = nms

    override fun addReloadStartHandler(runnable: Runnable) {
        reloadStartTask += {
            runnable.run()
        }
    }

    override fun addReloadEndHandler(consumer: Consumer<ReloadResult>) {
        reloadEndTask += {
            consumer.accept(it)
        }
    }
}