/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.impl.fabric

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder
import kr.toxicity.model.BetterModelEvaluatorImpl
import kr.toxicity.model.BetterModelEventBusImpl
import kr.toxicity.model.BetterModelPlatformImpl
import kr.toxicity.model.api.*
import kr.toxicity.model.api.BetterModelPlatform.ReloadResult.*
import kr.toxicity.model.api.event.PluginEndReloadEvent
import kr.toxicity.model.api.event.PluginStartReloadEvent
import kr.toxicity.model.api.manager.*
import kr.toxicity.model.api.mod.BetterModelMod
import kr.toxicity.model.api.mod.platform.ModAdapter
import kr.toxicity.model.api.mod.scheduler.ModModelScheduler
import kr.toxicity.model.api.nms.NMS
import kr.toxicity.model.api.pack.PackResult
import kr.toxicity.model.api.pack.PackZipper
import kr.toxicity.model.api.platform.PlatformAdapter
import kr.toxicity.model.api.version.MinecraftVersion
import kr.toxicity.model.impl.fabric.attachment.BetterModelAttachments
import kr.toxicity.model.impl.fabric.command.startFabricCommand
import kr.toxicity.model.impl.fabric.config.BetterModelConfigImpl
import kr.toxicity.model.impl.fabric.config.toConfig
import kr.toxicity.model.impl.fabric.manager.EntityManager
import kr.toxicity.model.impl.fabric.manager.PlayerManagerImpl
import kr.toxicity.model.impl.fabric.scheduler.FabricModelSchedulerImpl
import kr.toxicity.model.manager.*
import kr.toxicity.model.util.*
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.format.NamedTextColor.AQUA
import net.kyori.adventure.text.format.NamedTextColor.GREEN
import net.minecraft.DetectedVersion
import net.minecraft.WorldVersion
import net.minecraft.server.MinecraftServer
import net.minecraft.server.packs.metadata.pack.PackFormat
import net.minecraft.util.InclusiveRange
import org.semver4j.Semver
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.jar.JarFile
import kotlin.io.path.exists
import kotlin.system.measureTimeMillis

class BetterModelFabricImpl : ModInitializer, BetterModelPlatformImpl, BetterModelMod {
    private lateinit var server: MinecraftServer

    private val configDir: Path = FabricLoader.getInstance()
        .configDir
        .resolve(modId()).apply {
            toFile().mkdirs()
        }

    private val jarFile: JarFile
        get() = JarFile(
            File(javaClass.protectionDomain.codeSource.location.toURI())
        )

    private lateinit var config: BetterModelConfigImpl

    private val worldVersion: WorldVersion = DetectedVersion.tryDetectVersion()
    private val minecraftVersion: MinecraftVersion = MinecraftVersion.parse(worldVersion.id())

    private val semver: Semver = FabricLoader.getInstance()
        .getModContainer(modId())
        .map { modContainer ->
            Semver.coerce(modContainer.metadata.version.friendlyString)
                .ifNull { "Unable to load BetterModel's semver." }
        }
        .orElseThrow()

    private val nms by lazy {
        BetterModelNMSImpl()
    }
    private val logger = BetterModelLoggerImpl()
    private val evaluator = BetterModelEvaluatorImpl()
    private val eventBus = BetterModelEventBusImpl()
    private val adapter = ModAdapter()

    private var reloadStartTask: (PackZipper) -> Unit = { zipper ->
        callEvent {
            PluginStartReloadEvent(zipper)
        }
    }

    private var reloadEndTask: (BetterModelPlatform.ReloadResult) -> Unit = { result ->
        callEvent {
            PluginEndReloadEvent(result)
        }
    }

    private val isLoadingProvider: AtomicBoolean = AtomicBoolean()
    private val isFirstLoadProvider: AtomicBoolean = AtomicBoolean()

    private val allManagers by lazy {
        listOf(
            ArmorManager,
            ProfileManagerImpl,
            SkinManagerImpl,
            ModelManagerImpl,
            PlayerManagerImpl,
            EntityManager,
            ScriptManagerImpl
        )
    }

    override fun onInitialize() {
        BetterModel.register(this)
        startFabricCommand()
        ServerLifecycleEvents.SERVER_STARTING.register { server ->
            this.server = server
        }

        PolymerResourcePackUtils.addModAssets(modId())
        PolymerResourcePackUtils.markAsRequired()

        config = loadOrSaveConfig()

        val initialLoad = AtomicBoolean()

        PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register { builder ->
            val isInitialLoad = initialLoad.compareAndSet(false, true)
            reload {
                includeAssets(builder, it.packResult)
                if (isInitialLoad) loadLog(it)
            }
        }

        ServerLifecycleEvents.SERVER_STARTED.register {
            allManagers.forEach {
                it.start()
            }
            if (initialLoad.compareAndSet(false, true)) reload { loadLog(it) }
        }

        BetterModelAttachments.init()
        FabricModelSchedulerImpl.init()

        ServerLifecycleEvents.SERVER_STOPPED.register {
            allManagers.forEach { manager ->
                manager.end()
            }
        }
    }

    private fun reload(callback: (Success) -> Unit) {
        when (val result = reload(ReloadInfo(true, Audience.empty()))) {
            is Failure -> result.throwable.handleException("Unable to load mod properly.")
            is OnReload -> throw RuntimeException("mod load failed.")
            is Success -> callback(result)
        }
    }

    private fun includeAssets(builder: ResourcePackBuilder, packResult: PackResult) {
        packResult.stream().forEach { packByte ->
            when (val path = packByte.path.path) {
                "pack.png", "pack.mcmeta" -> return@forEach
                else ->  builder.addData(path, packByte.bytes)
            }
        }
        packResult.meta().overlays?.entries?.forEach { entry ->
            if (entry.directory == "bettermodel_legacy") {
                return@forEach
            }

            val min = entry.minFormat.run { PackFormat(major, minor) }
            val max = entry.maxFormat.run { PackFormat(major, minor) }
            val range = InclusiveRange(min, max)

            builder.packMcMetaBuilder.addOverlay(range, entry.directory)
        }
    }

    private fun loadLog(success: Success) {
        info(
            "Mod is loaded. (${success.totalTime().withComma()} ms)".toComponent(GREEN),
            "Platform: Fabric".toComponent(AQUA)
        )
    }

    override fun getResource(fileName: String): InputStream? {
        return javaClass.getResourceAsStream("/$fileName")
    }

    override fun saveResource(fileName: String) {
        getResource(fileName)?.use { input ->
            Files.copy(input, configDir.resolve(fileName))
        }
    }

    override fun loadAssets(pipeline: ReloadPipeline, prefix: String, consumer: BiConsumer<String, InputStream>) {
        jarFile.use { jarFile ->
            val jarEntries = jarFile.entries()
                .asSequence()
                .filter { jarEntry ->
                    jarEntry.name.startsWith(prefix) &&
                        jarEntry.name.length > prefix.length + 1 &&
                        !jarEntry.isDirectory
                }
                .toList()

            pipeline.forEachParallel(
                jarEntries,
                { it.size }
            ) { jarEntry ->
                jarFile.getInputStream(jarEntry).use { stream ->
                    consumer.accept(jarEntry.name.substring(prefix.length + 1), stream)
                }
            }
        }
    }

    override fun dataFolder(): File = configDir.toFile()

    override fun jarType(): BetterModelPlatform.JarType = BetterModelPlatform.JarType.FABRIC

    override fun reload(info: ReloadInfo): BetterModelPlatform.ReloadResult {
        if (!isLoadingProvider.compareAndSet(false, true)) {
            return OnReload.INSTANCE
        }

        return runCatching {
            if (!info.skipConfig) {
                config = loadOrSaveConfig()
            }

            val zipper = PackZipper.zipper()
            reloadStartTask(zipper)

            val indicators = config().indicator().options.toIndicator(info)
            ReloadPipeline(indicators).use { pipeline ->
                val assetsTime = measureTimeMillis {
                    allManagers.forEach { manager ->
                        manager.reload(pipeline, zipper)
                    }
                }

                pipeline.run {
                    status = "Generating files..."
                    goal = zipper.size()
                }

                val isFirstLoad = isFirstLoadProvider.compareAndSet(false, true)
                val packResult = config().packType().toGenerator().create(zipper, pipeline)

                Success(
                    isFirstLoad,
                    assetsTime,
                    packResult
                )
            }

        }
            .getOrElse { throwable ->
                Failure(throwable)
            }
            .also { result ->
                isLoadingProvider.set(false)
                reloadEndTask(result)
            }
    }

    private fun loadOrSaveConfig(): BetterModelConfigImpl {
        return configDir.resolve("config.yml").run {
            if (!exists()) saveResource("config.yml")
            toConfig()
        }
    }

    override fun isSnapshot(): Boolean = !worldVersion.stable()

    override fun config(): BetterModelConfig = config

    override fun version(): MinecraftVersion = minecraftVersion

    override fun semver(): Semver = semver

    override fun nms(): NMS = nms

    override fun modelManager(): ModelManager = ModelManagerImpl

    override fun playerManager(): PlayerManager = PlayerManagerImpl

    override fun scriptManager(): ScriptManager = ScriptManagerImpl

    override fun skinManager(): SkinManager = SkinManagerImpl

    override fun profileManager(): ProfileManager = ProfileManagerImpl

    override fun addReloadStartHandler(consumer: Consumer<PackZipper>) {
        val oldHandler = reloadStartTask
        reloadStartTask = { zipper ->
            oldHandler(zipper)
            consumer.accept(zipper)
        }
    }

    override fun addReloadEndHandler(consumer: Consumer<BetterModelPlatform.ReloadResult>) {
        val oldHandler = reloadEndTask
        reloadEndTask = { result ->
            oldHandler(result)
            consumer.accept(result)
        }
    }

    override fun logger(): BetterModelLogger = logger

    override fun evaluator(): BetterModelEvaluator = evaluator

    override fun eventBus(): BetterModelEventBus = eventBus

    override fun server(): MinecraftServer = server

    override fun scheduler(): ModModelScheduler = FabricModelSchedulerImpl

    override fun adapter(): PlatformAdapter = adapter

    override fun isEnabled(): Boolean = true
}
