package kr.toxicity.model

import com.vdurmont.semver4j.Semver
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.BetterModelLogger
import kr.toxicity.model.api.BetterModelPlugin
import kr.toxicity.model.api.BetterModelPlugin.ReloadResult
import kr.toxicity.model.api.BetterModelPlugin.ReloadResult.*
import kr.toxicity.model.api.manager.*
import kr.toxicity.model.api.nms.NMS
import kr.toxicity.model.api.scheduler.ModelScheduler
import kr.toxicity.model.api.util.HttpUtil
import kr.toxicity.model.api.version.MinecraftVersion
import kr.toxicity.model.api.version.MinecraftVersion.*
import kr.toxicity.model.manager.*
import kr.toxicity.model.scheduler.PaperScheduler
import kr.toxicity.model.scheduler.StandardScheduler
import kr.toxicity.model.util.DATA_FOLDER
import kr.toxicity.model.util.forEachAsync
import kr.toxicity.model.util.handleException
import kr.toxicity.model.util.info
import kr.toxicity.model.util.registerListener
import kr.toxicity.model.util.warn
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
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
            ScriptManagerImpl,
            CommandManagerImpl
        )
    }

    private val scheduler = if (BetterModel.IS_PAPER) PaperScheduler() else StandardScheduler()
    private val log = object : BetterModelLogger {
        override fun info(vararg message: String) {
            val logger = logger
            synchronized(logger) {
                for (s in message) {
                    logger.info(s)
                }
            }
        }
        override fun warn(vararg message: String) {
            val logger = logger
            synchronized(logger) {
                for (s in message) {
                    logger.warning(s)
                }
            }
        }
    }
    @Suppress("DEPRECATION")
    private val semver = Semver(description.version, Semver.SemverType.LOOSE)
    private val audiences by lazy {
        BukkitAudiences.create(this)
    }

    private val reloadStartTask = arrayListOf<() -> Unit>()
    private val reloadEndTask = arrayListOf<(ReloadResult) -> Unit>()

    override fun onLoad() {
        BetterModel.inst(this)
    }

    override fun onEnable() {
        nms = when (version) {
            V1_21_5 -> kr.toxicity.model.nms.v1_21_R4.NMSImpl()
            V1_21_4 -> kr.toxicity.model.nms.v1_21_R3.NMSImpl()
            V1_21_2, V1_21_3 -> kr.toxicity.model.nms.v1_21_R2.NMSImpl()
            V1_21, V1_21_1 -> kr.toxicity.model.nms.v1_21_R1.NMSImpl()
            V1_20_5, V1_20_6 -> kr.toxicity.model.nms.v1_20_R4.NMSImpl()
            V1_20_3, V1_20_4 -> kr.toxicity.model.nms.v1_20_R3.NMSImpl()
            V1_20_2 -> kr.toxicity.model.nms.v1_20_R2.NMSImpl()
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
        val version = HttpUtil.versionList()
        val versionNoticeList = arrayListOf<Component>()
        version.release?.let {
            if (semver < it.versionNumber()) versionNoticeList += Component.text("New BetterModel release found: ").append(it.toURLComponent())
        }
        version.snapshot?.let {
            if (semver < it.versionNumber()) versionNoticeList += Component.text("New BetterModel snapshot found: ").append(it.toURLComponent())
        }
        if (versionNoticeList.isNotEmpty()) {
            registerListener(object : Listener {
                @EventHandler
                fun PlayerJoinEvent.join() {
                    if (!player.isOp) return
                    versionNoticeList.forEach {
                        audiences.player(player).sendMessage(it)
                    }
                }
            })
        }
        when (val result = reload(ReloadInfo(DATA_FOLDER.exists()))) {
            is Failure -> result.throwable.handleException("Unable to load plugin properly.")
            is OnReload -> throw RuntimeException("Plugin load failed.")
            is Success -> info(
                "Plugin is loaded. (${result.time} ms)",
                "Minecraft version: $version, NMS version: ${nms.version()}",
                "Platform: ${when {
                    BetterModel.IS_PURPUR -> "Purpur"
                    BetterModel.IS_PAPER -> "Paper"
                    else -> "Bukkit"
                }}"
            )
        }
    }

    override fun onDisable() {
        audiences.close()
        managers.forEach(GlobalManagerImpl::end)
    }

    override fun reload(): ReloadResult = reload(ReloadInfo.DEFAULT)
    private fun reload(info: ReloadInfo): ReloadResult {
        if (!onReload.compareAndSet(false, true)) return OnReload()
        reloadStartTask.forEach {
            it()
        }
        val result = runCatching {
            val time = System.currentTimeMillis()
            managers.forEach {
                it.reload(info)
            }
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

    override fun logger(): BetterModelLogger = log
    override fun scheduler(): ModelScheduler = scheduler
    override fun modelManager(): ModelManager = ModelManagerImpl
    override fun playerManager(): PlayerManager = PlayerManagerImpl
    override fun entityManager(): EntityManager = EntityManagerImpl
    override fun commandManager(): CommandManager = CommandManagerImpl
    override fun compatibilityManager(): CompatibilityManager = CompatibilityManagerImpl
    override fun configManager(): ConfigManager = ConfigManagerImpl
    override fun scriptManager(): ScriptManager = ScriptManagerImpl

    override fun version(): MinecraftVersion = version
    override fun semver(): Semver = semver
    override fun nms(): NMS = nms
    override fun audiences(): BukkitAudiences = audiences

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