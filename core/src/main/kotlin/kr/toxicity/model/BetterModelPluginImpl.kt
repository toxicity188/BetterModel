/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model

import com.vdurmont.semver4j.Semver
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.BetterModelConfig
import kr.toxicity.model.api.BetterModelEvaluator
import kr.toxicity.model.api.BetterModelLogger
import kr.toxicity.model.api.BetterModelPlugin.ReloadResult
import kr.toxicity.model.api.BetterModelPlugin.ReloadResult.*
import kr.toxicity.model.api.manager.*
import kr.toxicity.model.api.nms.NMS
import kr.toxicity.model.api.pack.PackZipper
import kr.toxicity.model.api.scheduler.ModelScheduler
import kr.toxicity.model.api.version.MinecraftVersion
import kr.toxicity.model.configuration.PluginConfiguration
import kr.toxicity.model.manager.*
import kr.toxicity.model.util.*
import net.kyori.adventure.text.format.NamedTextColor.*
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.server.ServerLoadEvent
import java.io.InputStream
import java.util.function.Consumer
import java.util.jar.JarEntry
import java.util.jar.JarFile

abstract class BetterModelPluginImpl : AbstractBetterModelPlugin() {

    private lateinit var props: BetterModelProperties

    override fun onLoad() {
        super.onLoad()
        props = runCatching {
            BetterModelProperties(this)
        }.getOrElse {
            warn(
                "Unable to start this plugin.".toComponent(),
                "Reason: ${it.message ?: "Unknown"}".toComponent(RED),
                "Stack trace: ${it.stackTraceToString()}".toComponent(RED),
                "Plugin will be automatically disabled.".toComponent(DARK_RED)
            )
            return Bukkit.getPluginManager().disablePlugin(this)
        }
    }

    override fun onEnable() {
        props.managers.forEach(GlobalManager::start)
        if (isSnapshot) warn(
            "This build is dev version: be careful to use it!".toComponent(),
            "Build number: ${props.snapshot}".toComponent(LIGHT_PURPLE)
        )
        registerListener(object : Listener {
            @EventHandler
            fun PlayerJoinEvent.join() {
                if (!player.isOp || !config().versionCheck()) return
                props.scheduler.asyncTask {
                    val result = LATEST_VERSION
                    player.audience().infoNotNull(
                        result.release
                            ?.takeIf { props.semver < it.versionNumber() }
                            ?.let { version -> componentOf("New BetterModel release found: ") { append(version.toURLComponent()) } },
                        result.snapshot
                            ?.takeIf { props.semver < it.versionNumber() }
                            ?.let { version -> componentOf("New BetterModel snapshot found: ") { append(version.toURLComponent()) } }
                    )
                }
            }
            @EventHandler
            fun ServerLoadEvent.load() {
                if (skipInitialReload || type != ServerLoadEvent.LoadType.STARTUP) return
                when (val result = reload(ReloadInfo(true, Bukkit.getConsoleSender()))) {
                    is Failure -> result.throwable.handleException("Unable to load plugin properly.")
                    is OnReload -> throw RuntimeException("Plugin load failed.")
                    is Success -> info(
                        "Plugin is loaded. (${result.totalTime().withComma()} ms)".toComponent(GREEN),
                        "Minecraft version: ${props.version}, NMS version: ${props.nms.version()}".toComponent(AQUA),
                        "Platform: ${when {
                            BetterModel.IS_FOLIA -> "Folia"
                            BetterModel.IS_PURPUR -> "Purpur"
                            BetterModel.IS_PAPER -> "Paper"
                            else -> "Bukkit"
                        }}".toComponent(AQUA)
                    )
                }
            }
        })
    }

    override fun onDisable() {
        if (!firstLoad.get()) return
        props.managers.forEach(GlobalManager::end)
        ADVENTURE_PLATFORM?.close()
    }

    override fun reload(info: ReloadInfo): ReloadResult {
        if (!onReload.compareAndSet(false, true)) return OnReload.INSTANCE
        return runCatching {
            if (!info.skipConfig) props.config = BetterModelConfigImpl(PluginConfiguration.CONFIG.create())
            val zipper = PackZipper.zipper().also(props.reloadStartTask)
            ReloadPipeline(
                config().indicator().options.toIndicator(info)
            ).use { pipeline ->
                val time = System.currentTimeMillis()
                props.managers.forEach {
                    it.reload(pipeline, zipper)
                }
                Success(
                    firstLoad.compareAndSet(false, true),
                    System.currentTimeMillis() - time,
                    config().packType().toGenerator().create(zipper, pipeline.apply {
                        status = "Generating files..."
                        goal = zipper.size()
                    })
                )
            }
        }.getOrElse {
            Failure(it)
        }.apply {
            onReload.set(false)
        }.also(props.reloadEndTask)
    }

    fun loadAssets(pipeline: ReloadPipeline, prefix: String, consumer: (String, InputStream) -> Unit) {
        JarFile(file).use {
            pipeline.forEachParallel(it.entries()
                .asSequence()
                .filter { entry ->
                    entry.name.startsWith(prefix)
                            && entry.name.length > prefix.length + 1
                            && !entry.isDirectory
                }
                .toList(),
                JarEntry::getSize
            ) { entry ->
                it.getInputStream(entry).use { stream ->
                    consumer(entry.name.substring(prefix.length + 1), stream)
                }
            }
        }
    }

    override fun logger(): BetterModelLogger = logger
    override fun scheduler(): ModelScheduler = props.scheduler
    override fun evaluator(): BetterModelEvaluator = props.evaluator
    override fun modelManager(): ModelManager = ModelManagerImpl
    override fun playerManager(): PlayerManager = PlayerManagerImpl
    override fun scriptManager(): ScriptManager = ScriptManagerImpl
    override fun skinManager(): SkinManager = SkinManagerImpl
    override fun profileManager(): ProfileManager = ProfileManagerImpl

    override fun config(): BetterModelConfig = props.config
    override fun version(): MinecraftVersion = props.version
    override fun semver(): Semver = props.semver
    override fun nms(): NMS = props.nms
    override fun isSnapshot(): Boolean = props.snapshot > 0

    @Synchronized
    override fun addReloadStartHandler(consumer: Consumer<PackZipper>) {
        val previous = props.reloadStartTask
        props.reloadStartTask = {
            previous(it)
            consumer.accept(it)
        }
    }

    @Synchronized
    override fun addReloadEndHandler(consumer: Consumer<ReloadResult>) {
        val previous = props.reloadEndTask
        props.reloadEndTask = {
            previous(it)
            consumer.accept(it)
        }
    }
}
