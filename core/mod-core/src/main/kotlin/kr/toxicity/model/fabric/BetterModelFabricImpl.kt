/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.fabric

import com.vdurmont.semver4j.Semver
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils
import kr.toxicity.model.BetterModelEvaluatorImpl
import kr.toxicity.model.BetterModelEventBusImpl
import kr.toxicity.model.BetterModelPlatformImpl
import kr.toxicity.model.api.*
import kr.toxicity.model.api.event.PluginEndReloadEvent
import kr.toxicity.model.api.event.PluginStartReloadEvent
import kr.toxicity.model.api.fabric.BetterModelFabric
import kr.toxicity.model.api.fabric.platform.FabricAdapter
import kr.toxicity.model.api.manager.*
import kr.toxicity.model.api.nms.NMS
import kr.toxicity.model.api.pack.PackZipper
import kr.toxicity.model.api.platform.PlatformAdapter
import kr.toxicity.model.api.platform.PlatformRegionHolder
import kr.toxicity.model.api.scheduler.ModelScheduler
import kr.toxicity.model.api.version.MinecraftVersion
import kr.toxicity.model.fabric.attachment.BetterModelAttachments
import kr.toxicity.model.fabric.config.BetterModelConfigImpl
import kr.toxicity.model.fabric.config.BetterModelConfigManager
import kr.toxicity.model.fabric.manager.EntityManager
import kr.toxicity.model.fabric.manager.PlayerManagerImpl
import kr.toxicity.model.fabric.scheduler.FabricModelSchedulerManager
import kr.toxicity.model.manager.*
import kr.toxicity.model.util.callEvent
import kr.toxicity.model.util.toGenerator
import kr.toxicity.model.util.toIndicator
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.DetectedVersion
import net.minecraft.WorldVersion
import net.minecraft.server.MinecraftServer
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.jar.JarFile
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.system.measureTimeMillis

class BetterModelFabricImpl :
    ModInitializer,
    BetterModelPlatformImpl,
    BetterModelFabric
{
    private lateinit var server: MinecraftServer

    private val configDir: Path = FabricLoader.getInstance()
        .configDir
        .resolve(modId())

    private val jarFile: JarFile
        get() = JarFile(
            File(javaClass.getProtectionDomain().codeSource.location.toURI())
        )

    private var config: BetterModelConfigImpl = loadOrSaveConfig()

    private val worldVersion: WorldVersion = DetectedVersion.tryDetectVersion()
    private val minecraftVersion: MinecraftVersion = MinecraftVersion.parse(worldVersion.id())

    private val semver: Semver = FabricLoader.getInstance()
        .getModContainer(modId())
        .map { modContainer ->
            Semver(
                modContainer.metadata.version.friendlyString,
                Semver.SemverType.LOOSE
            )
        }
        .orElseThrow()

    private val nms = BetterModelNMSImpl()
    private val logger = BetterModelLoggerImpl()

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

    private val allManagers: List<GlobalManager> = listOf(
        ArmorManager,
        ProfileManagerImpl,
        SkinManagerImpl,
        ModelManagerImpl,
        PlayerManagerImpl,
        EntityManager,
        ScriptManagerImpl
    )

    override fun onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register { server ->
            this.server = server
        }

        PolymerResourcePackUtils.addModAssets(modId())
        PolymerResourcePackUtils.markAsRequired()

        BetterModelAttachments.init()
        FabricModelSchedulerManager.init()

        allManagers.forEach { manager ->
            manager.start()
        }
    }

    override fun getResource(fileName: String): InputStream? {
        val path = configDir.resolve(fileName)
        return if (path.exists()) {
            path.inputStream()
        } else {
            null
        }
    }

    override fun saveResource(fileName: String) {
        val path = configDir.resolve(fileName)
        if (path.exists()) {
            return
        }

        val inputStream = javaClass.getResourceAsStream("/$fileName")
            ?: throw IllegalStateException("Resource not found: $fileName")

        inputStream.use { input ->
            Files.copy(input, path)
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
            return BetterModelPlatform.ReloadResult.OnReload.INSTANCE
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

                BetterModelPlatform.ReloadResult.Success(
                    isFirstLoad,
                    assetsTime,
                    packResult
                )
            }

        }
            .getOrElse { throwable ->
                BetterModelPlatform.ReloadResult.Failure(throwable)
            }
            .also { result ->
                isLoadingProvider.set(false)
                reloadEndTask(result)
            }
    }

    private fun loadOrSaveConfig(): BetterModelConfigImpl {
        return BetterModelConfigManager.loadOrSave(
            "config",
            BetterModelConfigImpl::class
        ) {
            BetterModelConfigImpl()
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

    override fun evaluator(): BetterModelEvaluator = BetterModelEvaluatorImpl()

    override fun eventBus(): BetterModelEventBus = BetterModelEventBusImpl()

    override fun server(): MinecraftServer = server

    override fun regionHolder(): PlatformRegionHolder = FabricModelSchedulerManager.get()

    override fun scheduler(): ModelScheduler = FabricModelSchedulerManager.get()

    override fun adapter(): PlatformAdapter = FabricAdapter()
}
